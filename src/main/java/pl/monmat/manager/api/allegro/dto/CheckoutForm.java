package pl.monmat.manager.api.allegro.dto;

import java.util.List;

public record CheckoutForm(
        String id,
        Buyer buyer,
        List<LineItem> lineItems,
        Payment payment,
        Delivery delivery,
        Invoice invoice,
        String status,
        Summary summary,
        String note,
        String updatedAt
) {
    public record Delivery(
            DeliveryAddress address,
            DeliveryMethod method,
            Price cost,
            boolean smart,
            PickupPoint pickupPoint
    ) {}

    public record PickupPoint(
            String id,
            String name,
            String description
    ) {}

    public record DeliveryAddress(
            String firstName,
            String lastName,
            String street,
            String city,
            String zipCode,
            String countryCode,
            String phoneNumber
    ) {}

    public record DeliveryMethod(
            String id,
            String name
    ) {}

    public record Invoice(
            boolean required,
            InvoiceAddress address
    ) {}

    public record InvoiceAddress(
            String street,
            String city,
            String zipCode,
            String countryCode,
            InvoiceCompany company,
            InvoiceNaturalPerson naturalPerson
    ) {}

    public record InvoiceCompany(
            String name,
            String taxId
    ) {}

    public record InvoiceNaturalPerson(
            String firstName,
            String lastName
    ) {}

    public record Summary(
            Price totalToPay
    ) {}
}

