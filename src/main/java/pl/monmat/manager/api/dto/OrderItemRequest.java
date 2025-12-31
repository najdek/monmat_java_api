package pl.monmat.manager.api.dto;

import java.math.BigDecimal;

public record OrderItemRequest(
    String offerId,
    String name,
    int quantity,
    BigDecimal unitPrice,
    String unitPriceCurrency
) {}
