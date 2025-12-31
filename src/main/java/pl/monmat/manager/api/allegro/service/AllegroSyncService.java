package pl.monmat.manager.api.allegro.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import pl.monmat.manager.api.allegro.dto.AllegroOfferDetails;
import pl.monmat.manager.api.allegro.dto.CheckoutForm;
import pl.monmat.manager.api.allegro.dto.CheckoutFormsResponse;
import pl.monmat.manager.api.allegro.dto.LineItem;
import pl.monmat.manager.api.dto.CreateOrderRequest;
import pl.monmat.manager.api.dto.OrderItemRequest;
import pl.monmat.manager.api.service.OrderService;
import pl.monmat.manager.api.service.ProductAttributeParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllegroSyncService {
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

            // fetch orders from Allegro API
            CheckoutFormsResponse response = apiClient.get()
                    .uri("/order/checkout-forms?status=READY_FOR_PROCESSING")
                    .headers(h -> {
                        h.setBearerAuth(authService.getAccessToken());
                        h.set("Accept", "application/vnd.allegro.public.v1+json");
                    })
                    .retrieve().body(CheckoutFormsResponse.class);

            if (reponse == null || response.checkoutForms() == null) {
                return;
            }

            // process each order
            for (CheckoutForm form : response.checkoutForms()) {
                processSingleOrder(form, token);
            }
        } catch (Exception e) {
            System.err.println("Error during Allegro order sync: " + e.getMessage());
        }
    }

    private void processSingleOrder(CheckoutForm form, String token) {
        try {
            List<OrderItemRequest> items = new ArrayList<>();

            // fetch item details for each line item
            for (LineItem lineItem : form.lineItems()) {
                AllegroOfferDetails details = apiClient.get()
                        .uri("/sale/product-offers/" + lineItem.offer().id())
                        .headers(h -> {
                            h.setBearerAuth(token);
                            h.set("Accept", "application/vnd.allegro.public.v1+json");
                        })
                        .retrieve().body(AllegroOfferDetails.class);

                Map<String, Object> attributes = attributeParser.extractAttributes(details);

                items.add(new OrderItemRequest(
                        lineItem.offer().id(),
                        lineItem.offer().name(),
                        lineItem.quantity(),
                        new BigDecimal(lineItem.price().amount()),
                        lineItem.price().currency(),
                        attributes
                ));
            }

            // create order request
            CreateOrderRequest req = new CreateOrderRequest(
                    form.id(),
                    form.buyer().email(),
                    form.buyer().phoneNumber(),
                    form.createdAt(),
                    new BigDecimal(form.shipping().cost().amount()),
                    form.shipping().address().street(),
                    form.shipping().address().city(),
                    items
            );

        } catch (DataIntegrityViolationException e) {
            // order already exists, skip
        }
    }
}
