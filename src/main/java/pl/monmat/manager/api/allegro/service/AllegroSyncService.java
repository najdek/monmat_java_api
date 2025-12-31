package pl.monmat.manager.api.allegro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pl.monmat.manager.api.Order;
import pl.monmat.manager.api.allegro.dto.*;
import pl.monmat.manager.api.dto.CreateOrderRequest;
import pl.monmat.manager.api.dto.OrderItemRequest;
import pl.monmat.manager.api.json.Address;
import pl.monmat.manager.api.json.InvoiceDetails;
import pl.monmat.manager.api.service.OrderService;
import pl.monmat.manager.api.service.ProductAttributeParser;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AllegroSyncService {
    private static final Logger log = LoggerFactory.getLogger(AllegroSyncService.class);

    private final AllegroAuthService authService;
    private final OrderService orderService;
    private final ProductAttributeParser attributeParser;
    private final RestClient apiClient;

    public AllegroSyncService(
            AllegroAuthService auth,
            OrderService orderSvc,
            ProductAttributeParser parser,
            RestClient.Builder clientBuilder
    ) {
        this.authService = auth;
        this.orderService = orderSvc;
        this.attributeParser = parser;
        this.apiClient = clientBuilder.baseUrl("https://api.allegro.pl").build();
    }

    @Scheduled(fixedDelay = 180000) // 3 minutes
    public void syncOrders() {
        try {
            String token = authService.getAccessToken();

            // Collect orders from multiple pages (newest first from API)
            List<CheckoutForm> allOrders = new ArrayList<>();
            int limit = 100;
            int maxPages = 1; // if needed, can be increased to fetch more pages

            for (int page = 0; page < maxPages; page++) {
                int offset = page * limit;

                CheckoutFormsResponse response = apiClient.get()
                        .uri("/order/checkout-forms?status=READY_FOR_PROCESSING&sort=-lineItems.boughtAt&limit=" + limit + "&offset=" + offset)
                        .headers(h -> {
                            h.setBearerAuth(token);
                            h.set("Accept", "application/vnd.allegro.public.v1+json");
                        })
                        .retrieve().body(CheckoutFormsResponse.class);

                if (response == null || response.checkoutForms() == null || response.checkoutForms().isEmpty()) {
                    log.debug("No more orders at offset {}", offset);
                    break;
                }

                allOrders.addAll(response.checkoutForms());
                log.debug("Fetched {} orders at offset {} (total so far: {})", response.checkoutForms().size(), offset, allOrders.size());

                // If we got less than limit, there are no more pages
                if (response.checkoutForms().size() < limit) {
                    break;
                }
            }

            if (allOrders.isEmpty()) {
                log.debug("No orders to sync");
                return;
            }

            // Reverse the list to process oldest first (for correct customId assignment)
            Collections.reverse(allOrders);

            log.info("Found {} orders to sync (processing oldest first)", allOrders.size());

            // process each order (now from oldest to newest)
            for (CheckoutForm form : allOrders) {
                processSingleOrder(form, token);
            }
        } catch (Exception e) {
            log.error("Error during Allegro order sync: {}", e.getMessage());
        }
    }

    private void processSingleOrder(CheckoutForm form, String token) {
        try {
            log.debug("Processing order: {}", form.id());

            List<OrderItemRequest> items = new ArrayList<>();
            Instant earliestBoughtAt = null;

            // fetch item details for each line item
            if (form.lineItems() != null) {
                for (LineItem lineItem : form.lineItems()) {
                    if (lineItem.offer() == null) continue;

                    // Track earliest boughtAt from line items
                    if (lineItem.boughtAt() != null) {
                        if (earliestBoughtAt == null || lineItem.boughtAt().isBefore(earliestBoughtAt)) {
                            earliestBoughtAt = lineItem.boughtAt();
                        }
                    }

                    AllegroOfferDetails details = null;
                    try {
                        details = apiClient.get()
                                .uri("/sale/product-offers/" + lineItem.offer().id())
                                .headers(h -> {
                                    h.setBearerAuth(token);
                                    h.set("Accept", "application/vnd.allegro.public.v1+json");
                                })
                                .retrieve().body(AllegroOfferDetails.class);
                    } catch (Exception e) {
                        log.warn("Could not fetch offer details for {}: {}", lineItem.offer().id(), e.getMessage());
                    }

                    Map<String, Object> attributes = (details != null)
                            ? attributeParser.extractAttributes(details)
                            : Map.of();

                    String offerName = lineItem.offer().name() != null ? lineItem.offer().name() : "Unknown";
                    BigDecimal price = lineItem.price() != null && lineItem.price().amount() != null
                            ? new BigDecimal(lineItem.price().amount())
                            : BigDecimal.ZERO;
                    String currency = lineItem.price() != null && lineItem.price().currency() != null
                            ? lineItem.price().currency()
                            : "PLN";

                    items.add(new OrderItemRequest(
                            lineItem.offer().id(),
                            offerName,
                            lineItem.quantity(),
                            price,
                            currency,
                            attributes
                    ));
                }
            }

            // === SHIPPING ADDRESS ===
            Address shippingAddress = new Address();
            String deliveryPhone = "";

            if (form.delivery() != null && form.delivery().address() != null) {
                CheckoutForm.DeliveryAddress addr = form.delivery().address();
                shippingAddress.setFirstName(addr.firstName());
                shippingAddress.setLastName(addr.lastName());
                shippingAddress.setStreet(addr.street());
                shippingAddress.setCity(addr.city());
                shippingAddress.setZipCode(addr.zipCode());
                shippingAddress.setCountryCode(addr.countryCode() != null ? addr.countryCode() : "PL");
                shippingAddress.setPhoneNumber(addr.phoneNumber());
                deliveryPhone = addr.phoneNumber() != null ? addr.phoneNumber() : "";
            }

            // === BUYER INFO ===
            String email = "";
            String username = "";
            Boolean isGuest = null;
            String buyerPhone = "";

            if (form.buyer() != null) {
                email = form.buyer().email() != null ? form.buyer().email() : "";
                username = form.buyer().login() != null ? form.buyer().login() : "";
                isGuest = form.buyer().guest();
                buyerPhone = form.buyer().phoneNumber() != null ? form.buyer().phoneNumber() : "";

                // If no company name in delivery, try from buyer
                if (shippingAddress.getCompanyName() == null && form.buyer().companyName() != null) {
                    shippingAddress.setCompanyName(form.buyer().companyName());
                }
            }

            // Prefer phone from delivery address, fallback to buyer's phone
            String phone = !deliveryPhone.isEmpty() ? deliveryPhone : buyerPhone;

            // === PAYMENT INFO ===
            BigDecimal totalAmount = BigDecimal.ZERO;
            String paidCurrency = "PLN";
            LocalDateTime paymentAt = null;

            if (form.summary() != null && form.summary().totalToPay() != null) {
                if (form.summary().totalToPay().amount() != null) {
                    totalAmount = new BigDecimal(form.summary().totalToPay().amount());
                }
                if (form.summary().totalToPay().currency() != null) {
                    paidCurrency = form.summary().totalToPay().currency();
                }
            }

            if (form.payment() != null && form.payment().finishedAt() != null) {
                paymentAt = LocalDateTime.ofInstant(form.payment().finishedAt(), ZoneId.systemDefault());
            }

            // === SHIPPING COST ===
            BigDecimal shippingCost = BigDecimal.ZERO;
            String shippingCostCurrency = "PLN";

            if (form.delivery() != null && form.delivery().cost() != null) {
                if (form.delivery().cost().amount() != null) {
                    shippingCost = new BigDecimal(form.delivery().cost().amount());
                }
                if (form.delivery().cost().currency() != null) {
                    shippingCostCurrency = form.delivery().cost().currency();
                }
            }

            // === DELIVERY METHOD ===
            String deliveryMethodId = null;
            String deliveryMethodName = null;
            String pickupPointId = null;
            Boolean isSmart = null;

            if (form.delivery() != null) {
                isSmart = form.delivery().smart();
                if (form.delivery().method() != null) {
                    deliveryMethodId = form.delivery().method().id();
                    deliveryMethodName = form.delivery().method().name();
                }
                if (form.delivery().pickupPoint() != null) {
                    pickupPointId = form.delivery().pickupPoint().id();
                }
            }

            // === INVOICE ===
            Boolean needsInvoice = null;
            InvoiceDetails invoiceDetails = null;

            if (form.invoice() != null) {
                needsInvoice = form.invoice().required();

                if (form.invoice().required() && form.invoice().address() != null) {
                    invoiceDetails = new InvoiceDetails();
                    invoiceDetails.setNeedsInvoice(true);

                    CheckoutForm.InvoiceAddress invAddr = form.invoice().address();
                    invoiceDetails.setStreet(invAddr.street());
                    invoiceDetails.setCity(invAddr.city());
                    invoiceDetails.setZipCode(invAddr.zipCode());
                    invoiceDetails.setCountryCode(invAddr.countryCode());

                    if (invAddr.company() != null) {
                        invoiceDetails.setCompanyName(invAddr.company().name());
                        invoiceDetails.setTaxId(invAddr.company().taxId());
                    }
                }
            }

            // === BOUGHT AT ===
            LocalDateTime boughtAt = earliestBoughtAt != null
                    ? LocalDateTime.ofInstant(earliestBoughtAt, ZoneId.systemDefault())
                    : LocalDateTime.now();

            log.info("Order {} - earliestBoughtAt: {}, converted boughtAt: {}", form.id(), earliestBoughtAt, boughtAt);

            // === CREATE ORDER REQUEST ===
            CreateOrderRequest req = new CreateOrderRequest(
                    form.id(),                  // externalOrderId
                    email,                      // email
                    boughtAt,                   // boughtAt
                    phone,                      // phoneNumber
                    username,                   // username
                    isGuest,                    // isGuest
                    shippingAddress,            // shippingAddress (full Address object)
                    totalAmount,                // totalPaidAmount
                    paidCurrency,               // paidCurrency
                    paymentAt,                  // paymentAt
                    shippingCost,               // shippingCost
                    shippingCostCurrency,       // shippingCostCurrency
                    deliveryMethodId,           // deliveryMethodId
                    deliveryMethodName,         // deliveryMethodName
                    pickupPointId,              // pickupPointId
                    isSmart,                    // isSmart
                    needsInvoice,               // needsInvoice
                    invoiceDetails,             // invoiceDetails
                    form.note(),                // customerComment (note from Allegro API)
                    items                       // items
            );

            // save order to database (returns existing if already exists)
            Order savedOrder = orderService.createOrder(req);
            if (savedOrder != null && savedOrder.getCreatedAt() != null
                    && savedOrder.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(5))) {
                log.debug("Order {} already exists (customId: {}), skipping", form.id(), savedOrder.getCustomId());
            } else {
                log.info("Successfully synced NEW order: {} (bought at: {}, customId: {})",
                        form.id(), boughtAt, savedOrder != null ? savedOrder.getCustomId() : "N/A");
            }

        } catch (DataIntegrityViolationException e) {
            // order already exists due to unique constraint, skip
            log.debug("Order {} already exists (constraint violation), skipping", form.id());
        } catch (Exception e) {
            log.error("Error processing order {}: {}", form.id(), e.getMessage(), e);
        }
    }
}
