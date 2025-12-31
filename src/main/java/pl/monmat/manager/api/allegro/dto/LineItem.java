package pl.monmat.manager.api.allegro.dto;

import java.time.Instant;

public record LineItem(
        String id,
        Offer offer,
        int quantity,
        Price originalPrice,
        Price price,
        Instant boughtAt
) {}
