package pl.monmat.manager.api.allegro.api;

import java.util.List;

public record CheckoutFormsResponse(List<CheckoutForm> checkoutForms, int count, int totalCount) {
}
