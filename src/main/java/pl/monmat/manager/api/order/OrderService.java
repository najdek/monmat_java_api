package pl.monmat.manager.api.order;

import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pl.monmat.manager.api.order.dto.CreateOrderRequest;
import pl.monmat.manager.api.order.dto.OrderItemRequest;
import pl.monmat.manager.api.order.dto.PatchOrderRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private static final DateTimeFormatter CUSTOM_ID_FORMATTER = DateTimeFormatter.ofPattern("yyMM");
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        if (request.externalOrderId() != null && !request.externalOrderId().isEmpty()) {
            Optional<Order> existing = orderRepository.findByExternalOrderId(request.externalOrderId());
            if (existing.isPresent()) {
                throw new DataIntegrityViolationException("Order with externalOrderId " + request.externalOrderId() + " already exists");
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
        order.setUsername(request.username());
        order.setIsGuest(request.isGuest());
        order.setTotalPaidAmount(request.totalPaidAmount());
        order.setPaidCurrency(request.paidCurrency());
        order.setPaymentAt(request.paymentAt());
        order.setShippingCost(request.shippingCost());
        order.setShippingCostCurrency(request.shippingCostCurrency());
        order.setDeliveryMethodId(request.deliveryMethodId());
        order.setDeliveryMethodName(request.deliveryMethodName());
        order.setPickupPointId(request.pickupPointId());
        order.setIsSmart(request.isSmart());
        order.setNeedsInvoice(request.needsInvoice());
        order.setInvoiceDetails(request.invoiceDetails());
        order.setCustomerComment(request.customerComment());
        order.setShippingAddress(request.shippingAddress());
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
                item.setAttributes(itemReq.attributes());
                item.setOrder(order);
                entityItems.add(item);
                if (itemReq.unitPrice() != null) {
                    BigDecimal lineTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
                    calculatedTotal = calculatedTotal.add(lineTotal);
                }
            }
        }
        order.setItems(entityItems);
        if (order.getTotalPaidAmount() == null) {
            order.setTotalPaidAmount(calculatedTotal);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Optional<Order> patchOrder(UUID uuid, PatchOrderRequest patch) {
        return orderRepository.findByUuid(uuid)
                .map(order -> applyPatch(order, patch))
                .map(orderRepository::save);
    }

    private Order applyPatch(Order order, PatchOrderRequest patch) {
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
        return order;
    }

    private String generateCustomId(LocalDateTime orderDateTime) {
        String prefix = CUSTOM_ID_FORMATTER.format(orderDateTime);
        return orderRepository.findLastOrderInMonth(prefix)
                .map(Order::getCustomId)
                .map(id -> {
                    int num = Integer.parseInt(id.split("/")[1]);
                    return prefix + "/" + String.format("%05d", num + 1);
                })
                .orElse(prefix + "/00001");
    }
}
