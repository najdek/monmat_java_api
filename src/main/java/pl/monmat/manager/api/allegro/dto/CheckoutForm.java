package pl.monmat.manager.api.allegro.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CheckoutForm(
        String id,
        Buyer buyer,
        List<LineItem> lineItems,
        Payment payment,
        Shipping shipping,
        String status,
        LocalDateTime boughtAt
) {}
