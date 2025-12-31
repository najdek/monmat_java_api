package pl.monmat.manager.api.allegro.dto;

public record Buyer(
        String email,
        String login,
        String phoneNumber,
        boolean guest
) {}
