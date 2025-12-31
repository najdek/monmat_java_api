package pl.monmat.manager.api.allegro.dto;

import java.util.List;

public record CheckoutFormsResponse(
        List<CheckoutForm> checkoutForms,
        int count,
        int totalCount
) {}

