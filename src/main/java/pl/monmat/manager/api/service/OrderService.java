package pl.monmat.manager.api.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pl.monmat.manager.api.Order;
import pl.monmat.manager.api.OrderItem;
import pl.monmat.manager.api.OrderRepository;
import pl.monmat.manager.api.dto.CreateOrderRequest;
import pl.monmat.manager.api.dto.OrderItemRequest;
import pl.monmat.manager.api.dto.PatchOrderRequest;
import pl.monmat.manager.api.json.Address;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional // all or nothing
    public Order createOrder(CreateOrderRequest request) {
        // Check if order with this external ID already exists
        if (request.externalOrderId() != null && !request.externalOrderId().isEmpty()) {
            if (orderRepository.existsByExternalOrderId(request.externalOrderId())) {
                // Order already exists, return existing one
                return orderRepository.findByExternalOrderId(request.externalOrderId()).orElse(null);
            }
        }

        Order order = new Order();
        order.setUuid(UUID.randomUUID());
        order.setExternalOrderId(request.externalOrderId());
        order.setEmail(request.email());
        order.setPhoneNumber(request.phoneNumber());
        order.setBoughtAt(request.boughtAt() != null ? request.boughtAt() : LocalDateTime.now());
        order.setCustomId(generateCustomId(order.getBoughtAt()));
        order.setStatus("NEW");

        // Buyer info
        order.setUsername(request.username());
        order.setIs_guest(request.isGuest());

        // Payment info
        order.setTotalPaidAmount(request.totalPaidAmount());
        order.setPaidCurrency(request.paidCurrency());
        order.setPaymentAt(request.paymentAt());

        // Shipping cost
        order.setShippingCost(request.shippingCost());
        order.setShippingCostCurrency(request.shippingCostCurrency());

        // Delivery method
        order.setDeliveryMethodId(request.deliveryMethodId());
        order.setDeliveryMethodName(request.deliveryMethodName());
        order.setPickupPointId(request.pickupPointId());
        order.setIsSmart(request.isSmart());

        // Invoice
        order.setNeedsInvoice(request.needsInvoice());
        if (request.invoiceDetails() != null) {
            order.setInvoiceDetails(request.invoiceDetails());
        }

        // Comments
        order.setCustomerComment(request.customerComment());

        // Shipping address from DTO
        if (request.shippingAddress() != null) {
            order.setShippingAddress(request.shippingAddress());
        }

        // Process items
        List<OrderItem> entityItems = new ArrayList<>();
        BigDecimal calculatedTotal = BigDecimal.ZERO;

        if (request.items() != null) {
            for (OrderItemRequest itemReq : request.items()) {
                OrderItem item = new OrderItem();
                item.setExternalOfferId(itemReq.offerId());
                item.setName(itemReq.name());
                item.setQuantity(itemReq.quantity());
                item.setUnitPrice(itemReq.unitPrice());
                item.setCurrency(itemReq.unitPriceCurrency());
                item.setOrder(order);
                entityItems.add(item);

                if (itemReq.unitPrice() != null) {
                    BigDecimal lineTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
                    calculatedTotal = calculatedTotal.add(lineTotal);
                }
            }
        }

        order.setItems(entityItems);

        // Use provided total or calculated total
        if (order.getTotalPaidAmount() == null) {
            order.setTotalPaidAmount(calculatedTotal);
        }

        return orderRepository.save(order);

    }

    @Transactional
    public Order patchOrder(UUID uuid, PatchOrderRequest patch) {
        Optional<Order> orderOpt = orderRepository.findByUuid(uuid);

        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order with UUID " + uuid + " not found");
        }

        Order order = orderOpt.get();

        // Update only non-null fields
        if (patch.trackingNumbers() != null) {
            order.setTrackingNumbers(patch.trackingNumbers());
        }
        if (patch.status() != null) {
            order.setStatus(patch.status());
        }
        if (patch.internalNotes() != null) {
            order.setInternalNotes(patch.internalNotes());
        }
        if (patch.customerComment() != null) {
            order.setCustomerComment(patch.customerComment());
        }
        if (patch.acceptedAt() != null) {
            order.setAcceptedAt(patch.acceptedAt());
        }
        if (patch.completedAt() != null) {
            order.setCompletedAt(patch.completedAt());
        }
        if (patch.shippedAt() != null) {
            order.setShippedAt(patch.shippedAt());
        }
        if (patch.deliveredAt() != null) {
            order.setDeliveredAt(patch.deliveredAt());
        }
        if (patch.deliveryMethodId() != null) {
            order.setDeliveryMethodId(patch.deliveryMethodId());
        }
        if (patch.deliveryMethodName() != null) {
            order.setDeliveryMethodName(patch.deliveryMethodName());
        }
        if (patch.pickupPointId() != null) {
            order.setPickupPointId(patch.pickupPointId());
        }

        return orderRepository.save(order);
    }

    private String generateCustomId(LocalDateTime orderDateTime) {
        String prefix = java.time.format.DateTimeFormatter.ofPattern("yyMM").format(orderDateTime);
        return orderRepository.findLastOrderInMonth(prefix)
                .map(Order::getCustomId)
                .map(id -> {
                    int num = Integer.parseInt(id.split("/")[1]);
                    return prefix + "/" + String.format("%05d", num + 1);
                })
                .orElse(prefix + "/00001");
    }


}
