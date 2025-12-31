package pl.monmat.manager.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateOrderRequest (
    String externalOrderId,
    @NotNull String email,
    LocalDateTime boughtAt,
    String phoneNumber,

    // Address
    String street,
    String city,
    String zipCode,
    String countryCode,

    BigDecimal shippingCost,

    List<OrderItemRequest> items

) {}

