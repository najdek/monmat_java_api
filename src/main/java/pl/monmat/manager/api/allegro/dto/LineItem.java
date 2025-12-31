package pl.monmat.manager.api.allegro.dto;

public record LineItem(
        String offerId,
        Offer offer,
        int quantity,
        Price price
) {}
