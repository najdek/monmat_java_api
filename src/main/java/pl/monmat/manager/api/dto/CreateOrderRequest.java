package pl.monmat.manager.api.dto;

import jakarta.validation.constraints.NotNull;
import pl.monmat.manager.api.json.Address;
import pl.monmat.manager.api.json.InvoiceDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateOrderRequest (
    String externalOrderId,
    @NotNull String email,
    LocalDateTime boughtAt,
    String phoneNumber,

    // Buyer info
    String username,
    Boolean isGuest,

    // Shipping Address (full)
    Address shippingAddress,

    // Payment info
    BigDecimal totalPaidAmount,
    String paidCurrency,
    LocalDateTime paymentAt,

    // Shipping
    BigDecimal shippingCost,
    String shippingCostCurrency,

    // Delivery method
    String deliveryMethodId,
    String deliveryMethodName,
    String pickupPointId,
    Boolean isSmart,

    // Invoice
    Boolean needsInvoice,
    InvoiceDetails invoiceDetails,

    // Comments
    String customerComment,

    // Items
    List<OrderItemRequest> items

) {}

