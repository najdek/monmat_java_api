package pl.monmat.manager.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public record OrderItemRequest(
    String offerId,
    String name,
    int quantity,
    BigDecimal unitPrice,
    String unitPriceCurrency,
    Map<String,Object> attributes) {}
