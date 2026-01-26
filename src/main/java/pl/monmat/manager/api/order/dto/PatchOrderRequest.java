package pl.monmat.manager.api.order.dto;

import java.time.LocalDateTime;

public record PatchOrderRequest(
        String trackingNumbers,
        String status,
        String internalNotes,
        String customerComment,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        String deliveryMethodId,
        String deliveryMethodName,
        String pickupPointId
) {
}
