package pl.monmat.manager.api.allegro.dto;

public record Buyer(
        String id,
        String email,
        String login,
        String firstName,
        String lastName,
        String companyName,
        String phoneNumber,
        boolean guest,
        BuyerAddress address
) {
    public record BuyerAddress(
            String street,
            String city,
            String postCode,
            String countryCode
    ) {}
}
