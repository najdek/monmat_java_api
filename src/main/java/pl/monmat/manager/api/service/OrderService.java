package pl.monmat.manager.api.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pl.monmat.manager.api.Order;
import pl.monmat.manager.api.OrderItem;
import pl.monmat.manager.api.OrderRepository;
import pl.monmat.manager.api.dto.CreateOrderRequest;
import pl.monmat.manager.api.dto.OrderItemRequest;
import pl.monmat.manager.api.json.Address;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional // all or nothing
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setUuid(UUID.randomUUID());
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
            item.setCurrency("PLN"); // change later

            item.setOrder(order);

            entityItems.add(item);

            BigDecimal lineTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setItems(entityItems);
        order.setTotalPaidAmount(totalAmount);

        return orderRepository.save(order);

    }


}
