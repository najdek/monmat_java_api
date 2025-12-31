package pl.monmat.manager.api.allegro.dto;

public record Shipping (
        String shippingMethodId,
        String shippingMethodName,
        ShippingAddress shippingAddress
) {}

record ShippingAddress (
        String firstName,
        String lastName,
        String street,
        String houseNumber,
        String apartmentNumber,
        String postalCode,
        String city,
        String countryCode,
        String phoneNumber,
        String email
) {}