package pl.monmat.manager.api.dto;

import java.time.LocalDateTime;

public record PatchOrderRequest(
        String trackingNumbers,
        String status,
        String internalNotes,
        String customerComment,
        LocalDateTime shippedAt,
        LocalDateTime completedAt,
        String deliveryMethodId,
        String deliveryMethodName,
        String pickupPointId
) {
}

