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
        Order order = new Order();
        order.setUuid(UUID.randomUUID());
        order.setExternalOrderId(request.externalOrderId());
        order.setEmail(request.email());
        order.setPhoneNumber(request.phoneNumber());
        order.setBoughtAt(LocalDateTime.now());
        order.setStatus("NEW");

        order.setShippingCost(request.shippingCost());

        // map address from DTO to JSONB

        Address addr = new Address();
        addr.setStreet(request.street());
        addr.setCity(request.city());
        // add rest later
        order.setShippingAddress(addr);

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderItem> entityItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.items()) {
            OrderItem item = new OrderItem();
            item.setExternalOfferId(itemReq.offerId());
            item.setName(itemReq.name());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            item.setCurrency(itemReq.unitPriceCurrency());

            item.setOrder(order);

            entityItems.add(item);

            BigDecimal lineTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setItems(entityItems);
        order.setTotalPaidAmount(totalAmount);

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


}
