package pl.monmat.manager.api.order;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import pl.monmat.manager.api.order.dto.CreateOrderRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void shouldCreateOrderInDatabase() {
        // Given
        String externalOrderId = UUID.randomUUID().toString();
        CreateOrderRequest request = new CreateOrderRequest(
                externalOrderId, "integration@test.com", null, "987654321", "IntUser",
                false, null, BigDecimal.valueOf(200), "USD", null, BigDecimal.ZERO, "USD",
                "pickup", "Pickup Point", "P1", true, false, null, "Comment", null);

        // When
        Order response = orderService.createOrder(request);
        entityManager.flush();
        entityManager.clear(); // this forces Hibernate to reload the entity from the DB

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getExternalOrderId()).isEqualTo(externalOrderId);

        // Verify in DB
        Order savedOrder = orderRepository.findById(response.getId()).orElseThrow();
        assertThat(savedOrder.getEmail()).isEqualTo("integration@test.com");
        assertThat(savedOrder.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    @Transactional
    void shouldThrowOnCreatingOrderWithDuplicateExternalId() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                "int-test-1", "integration@test.com", null, "987654321", "IntUser",
                false, null, BigDecimal.valueOf(200), "USD", null, BigDecimal.ZERO, "USD",
                "pickup", "Pickup Point", "P1", true, false, null, "Comment", null);

        orderService.createOrder(request);
        entityManager.flush();

        assertThatThrownBy(() -> {
            orderService.createOrder(request);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
