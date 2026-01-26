package pl.monmat.manager.api.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.monmat.manager.api.order.dto.CreateOrderRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderInDatabase() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                "int-test-1", "integration@test.com", null, "987654321", "IntUser",
                false, null, BigDecimal.valueOf(200), "USD", null, BigDecimal.ZERO, "USD",
                "pickup", "Pickup Point", "P1", true, false, null, "Comment", null);

        // When
        Order response = orderService.createOrder(request);

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getExternalOrderId()).isEqualTo("int-test-1");

        // Verify in DB
        Order savedOrder = orderRepository.findById(response.getId()).orElseThrow();
        assertThat(savedOrder.getEmail()).isEqualTo("integration@test.com");
        assertThat(savedOrder.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

}
