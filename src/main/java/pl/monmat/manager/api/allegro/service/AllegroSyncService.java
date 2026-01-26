package pl.monmat.manager.api.allegro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pl.monmat.manager.api.allegro.api.*;
import pl.monmat.manager.api.common.model.Address;
import pl.monmat.manager.api.common.model.InvoiceDetails;
import pl.monmat.manager.api.order.Order;
import pl.monmat.manager.api.order.OrderService;
import pl.monmat.manager.api.order.dto.CreateOrderRequest;
import pl.monmat.manager.api.order.dto.OrderItemRequest;

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
    private static final String DEFAULT_CURRENCY = "PLN";
    private static final int SYNC_LIMIT = 100;
    private static final int MAX_PAGES = 1;
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

    @Scheduled(fixedDelay = 180_000)
    public void syncOrders() {
        try {
            String token = authService.getAccessToken();
            List<CheckoutForm> allOrders = fetchAllOrders(token);
            if (allOrders.isEmpty()) {
                log.debug("No orders to sync");
                return;
            }
            Collections.reverse(allOrders);
            log.info("Found {} orders to sync (processing oldest first)", allOrders.size());
            for (CheckoutForm form : allOrders) {
                processSingleOrder(form, token);
            }
        } catch (Exception e) {
            log.error("Error during Allegro order sync: {}", e.getMessage());
        }
    }

    private List<CheckoutForm> fetchAllOrders(String token) {
        List<CheckoutForm> allOrders = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            int offset = page * SYNC_LIMIT;
            CheckoutFormsResponse response = fetchOrdersPage(token, offset);
            if (response == null || response.checkoutForms() == null || response.checkoutForms().isEmpty()) {
                break;
            }
            allOrders.addAll(response.checkoutForms());
            log.debug("Fetched {} orders at offset {} (total: {})", response.checkoutForms().size(), offset, allOrders.size());
            if (response.checkoutForms().size() < SYNC_LIMIT) {
                break;
            }
        }
        return allOrders;
    }

    private CheckoutFormsResponse fetchOrdersPage(String token, int offset) {
        return apiClient.get()
                .uri("/order/checkout-forms?status=READY_FOR_PROCESSING&sort=-lineItems.boughtAt&limit=" + SYNC_LIMIT + "&offset=" + offset)
                .headers(h -> {
                    h.setBearerAuth(token);
                    h.set("Accept", "application/vnd.allegro.public.v1+json");
                })
                .retrieve()
                .body(CheckoutFormsResponse.class);
    }

    private void processSingleOrder(CheckoutForm form, String token) {
        try {
            log.debug("Processing order: {}", form.id());
            List<OrderItemRequest> items = buildOrderItems(form, token);
            LocalDateTime boughtAt = extractBoughtAt(form);
            Address shippingAddress = extractShippingAddress(form);
            BuyerInfo buyerInfo = extractBuyerInfo(form, shippingAddress);
            PaymentInfo paymentInfo = extractPaymentInfo(form);
            DeliveryInfo deliveryInfo = extractDeliveryInfo(form);
            InvoiceInfo invoiceInfo = extractInvoiceInfo(form);
            CreateOrderRequest req = new CreateOrderRequest(
                    form.id(),
                    buyerInfo.email(),
                    boughtAt,
                    buyerInfo.phone(),
                    buyerInfo.username(),
                    buyerInfo.isGuest(),
                    shippingAddress,
                    paymentInfo.totalAmount(),
                    paymentInfo.currency(),
                    paymentInfo.paymentAt(),
                    deliveryInfo.shippingCost(),
                    deliveryInfo.shippingCostCurrency(),
                    deliveryInfo.methodId(),
                    deliveryInfo.methodName(),
                    deliveryInfo.pickupPointId(),
                    deliveryInfo.isSmart(),
                    invoiceInfo.needsInvoice(),
                    invoiceInfo.details(),
                    form.note(),
                    items
            );
            Order savedOrder = orderService.createOrder(req);
            logOrderResult(form, savedOrder);
        } catch (DataIntegrityViolationException e) {
            log.debug("Order {} already exists (constraint violation), skipping", form.id());
        } catch (Exception e) {
            log.error("Error processing order {}: {}", form.id(), e.getMessage(), e);
        }
    }

    private List<OrderItemRequest> buildOrderItems(CheckoutForm form, String token) {
        List<OrderItemRequest> items = new ArrayList<>();
        if (form.lineItems() == null) {
            return items;
        }
        for (LineItem lineItem : form.lineItems()) {
            if (lineItem.offer() == null) continue;
            Map<String, Object> attributes = fetchOfferAttributes(lineItem.offer().id(), token);
            String name = lineItem.offer().name() != null ? lineItem.offer().name() : "Unknown";
            BigDecimal price = extractPrice(lineItem.price());
            String currency = extractCurrency(lineItem.price());
            items.add(new OrderItemRequest(lineItem.offer().id(), name, lineItem.quantity(), price, currency, attributes));
        }
        return items;
    }

    private Map<String, Object> fetchOfferAttributes(String offerId, String token) {
        try {
            AllegroOfferDetails details = apiClient.get()
                    .uri("/sale/product-offers/" + offerId)
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.set("Accept", "application/vnd.allegro.public.v1+json");
                    })
                    .retrieve()
                    .body(AllegroOfferDetails.class);
            return details != null ? attributeParser.extractAttributes(details) : Map.of();
        } catch (Exception e) {
            log.warn("Could not fetch offer details for {}: {}", offerId, e.getMessage());
            return Map.of();
        }
    }

    private LocalDateTime extractBoughtAt(CheckoutForm form) {
        if (form.lineItems() == null) {
            return LocalDateTime.now();
        }
        Instant earliest = form.lineItems().stream()
                .filter(item -> item.boughtAt() != null)
                .map(LineItem::boughtAt)
                .min(Instant::compareTo)
                .orElse(null);
        return earliest != null ? LocalDateTime.ofInstant(earliest, ZoneId.systemDefault()) : LocalDateTime.now();
    }

    private Address extractShippingAddress(CheckoutForm form) {
        Address address = new Address();
        if (form.delivery() != null && form.delivery().address() != null) {
            CheckoutForm.DeliveryAddress addr = form.delivery().address();
            address.setFirstName(addr.firstName());
            address.setLastName(addr.lastName());
            address.setStreet(addr.street());
            address.setCity(addr.city());
            address.setZipCode(addr.zipCode());
            address.setCountryCode(addr.countryCode() != null ? addr.countryCode() : "PL");
            address.setPhoneNumber(addr.phoneNumber());
        }
        return address;
    }

    private BuyerInfo extractBuyerInfo(CheckoutForm form, Address shippingAddress) {
        final String deliveryPhone = shippingAddress.getPhoneNumber() != null ? shippingAddress.getPhoneNumber() : "";

        if (form.buyer() == null) {
            return new BuyerInfo("", "", null, deliveryPhone);
        }

        final String email = form.buyer().email() != null ? form.buyer().email() : "";
        final String username = form.buyer().login() != null ? form.buyer().login() : "";
        final Boolean isGuest = form.buyer().guest();
        final String buyerPhone = form.buyer().phoneNumber() != null ? form.buyer().phoneNumber() : "";

        if (shippingAddress.getCompanyName() == null && form.buyer().companyName() != null) {
            shippingAddress.setCompanyName(form.buyer().companyName());
        }

        final String phone = !deliveryPhone.isEmpty() ? deliveryPhone : buyerPhone;
        return new BuyerInfo(email, username, isGuest, phone);
    }

    private PaymentInfo extractPaymentInfo(CheckoutForm form) {
        final BigDecimal totalAmount;
        final String currency;
        if (form.summary() != null && form.summary().totalToPay() != null) {
            totalAmount = extractPrice(form.summary().totalToPay());
            currency = extractCurrency(form.summary().totalToPay());
        } else {
            totalAmount = BigDecimal.ZERO;
            currency = DEFAULT_CURRENCY;
        }

        final LocalDateTime paymentAt;
        if (form.payment() != null && form.payment().finishedAt() != null) {
            paymentAt = LocalDateTime.ofInstant(form.payment().finishedAt(), ZoneId.systemDefault());
        } else {
            paymentAt = null;
        }

        return new PaymentInfo(totalAmount, currency, paymentAt);
    }

    private DeliveryInfo extractDeliveryInfo(CheckoutForm form) {
        if (form.delivery() == null) {
            return new DeliveryInfo(BigDecimal.ZERO, DEFAULT_CURRENCY, null, null, null, null);
        }

        final Boolean isSmart = form.delivery().smart();

        final BigDecimal shippingCost;
        final String shippingCostCurrency;
        if (form.delivery().cost() != null) {
            shippingCost = extractPrice(form.delivery().cost());
            shippingCostCurrency = extractCurrency(form.delivery().cost());
        } else {
            shippingCost = BigDecimal.ZERO;
            shippingCostCurrency = DEFAULT_CURRENCY;
        }

        final String methodId;
        final String methodName;
        if (form.delivery().method() != null) {
            methodId = form.delivery().method().id();
            methodName = form.delivery().method().name();
        } else {
            methodId = null;
            methodName = null;
        }

        final String pickupPointId;
        if (form.delivery().pickupPoint() != null) {
            pickupPointId = form.delivery().pickupPoint().id();
        } else {
            pickupPointId = null;
        }

        return new DeliveryInfo(shippingCost, shippingCostCurrency, methodId, methodName, pickupPointId, isSmart);
    }

    private InvoiceInfo extractInvoiceInfo(CheckoutForm form) {
        if (form.invoice() == null) {
            return new InvoiceInfo(null, null);
        }

        final Boolean needsInvoice = form.invoice().required();
        final InvoiceDetails details = extractInvoiceDetails(form.invoice());

        return new InvoiceInfo(needsInvoice, details);
    }

    private InvoiceDetails extractInvoiceDetails(CheckoutForm.Invoice invoice) {
        if (!invoice.required() || invoice.address() == null) {
            return null;
        }

        final InvoiceDetails details = new InvoiceDetails();
        details.setNeedsInvoice(true);

        final CheckoutForm.InvoiceAddress addr = invoice.address();
        details.setStreet(addr.street());
        details.setCity(addr.city());
        details.setZipCode(addr.zipCode());
        details.setCountryCode(addr.countryCode());

        if (addr.company() != null) {
            details.setCompanyName(addr.company().name());
            details.setTaxId(addr.company().taxId());
        }

        return details;
    }

    private BigDecimal extractPrice(Price price) {
        if (price == null || price.amount() == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(price.amount());
    }

    private String extractCurrency(Price price) {
        if (price == null || price.currency() == null) {
            return DEFAULT_CURRENCY;
        }
        return price.currency();
    }

    private void logOrderResult(CheckoutForm form, Order savedOrder) {
        if (savedOrder != null && savedOrder.getCreatedAt() != null
                && savedOrder.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(5))) {
            log.debug("Order {} already exists (customId: {}), skipping", form.id(), savedOrder.getCustomId());
        } else {
            log.info("Successfully synced NEW order: {} (customId: {})",
                    form.id(), savedOrder != null ? savedOrder.getCustomId() : "N/A");
        }
    }

    private record BuyerInfo(String email, String username, Boolean isGuest, String phone) {
    }

    private record PaymentInfo(BigDecimal totalAmount, String currency, LocalDateTime paymentAt) {
    }

    private record DeliveryInfo(BigDecimal shippingCost, String shippingCostCurrency, String methodId,
                                String methodName, String pickupPointId, Boolean isSmart) {
    }

    private record InvoiceInfo(Boolean needsInvoice, InvoiceDetails details) {
    }
}
