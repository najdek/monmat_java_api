package pl.monmat.manager.api.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUuid(UUID uuid);

    Optional<Order> findByExternalOrderId(String externalOrderId);

    @Query(value = "SELECT * FROM orders WHERE custom_id LIKE CONCAT(:prefix, '%') ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Order> findLastOrderInMonth(String prefix);
}
