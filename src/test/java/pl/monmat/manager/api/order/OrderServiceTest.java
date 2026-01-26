package pl.monmat.manager.api.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.monmat.manager.api.order.dto.CreateOrderRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_ShouldSaveAndReturnOrder() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                "ext-123", "test@example.com", null, "123456789", "User1", false,
                null, BigDecimal.valueOf(100), "PLN", null, BigDecimal.TEN, "PLN",
                "courier", "Courier", null, false, false, null, "Note", null);
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setExternalOrderId("ext-123");

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        Order response = orderService.createOrder(request);

        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getExternalOrderId()).isEqualTo("ext-123");
        verify(orderRepository).save(any(Order.class));
    }
}