package pl.monmat.manager.api.allegro.dto;

public record Payment(
        String amount,
        String currency,
        String method,
        String status
) {}
