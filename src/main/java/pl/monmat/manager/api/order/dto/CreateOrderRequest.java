package pl.monmat.manager.api.order.dto;

import jakarta.validation.constraints.NotNull;
import pl.monmat.manager.api.common.model.Address;
import pl.monmat.manager.api.common.model.InvoiceDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateOrderRequest(
        String externalOrderId,
        @NotNull String email,
        LocalDateTime boughtAt,
        String phoneNumber,
        String username,
        Boolean isGuest,
        Address shippingAddress,
        BigDecimal totalPaidAmount,
        String paidCurrency,
        LocalDateTime paymentAt,
        BigDecimal shippingCost,
        String shippingCostCurrency,
        String deliveryMethodId,
        String deliveryMethodName,
        String pickupPointId,
        Boolean isSmart,
        Boolean needsInvoice,
        InvoiceDetails invoiceDetails,
        String customerComment,
        List<OrderItemRequest> items
) {
}
