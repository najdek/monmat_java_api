package pl.monmat.manager.api.order;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.monmat.manager.api.order.dto.CreateOrderRequest;
import pl.monmat.manager.api.order.dto.PatchOrderRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository repository;
    private final OrderService orderService;

    public OrderController(OrderRepository repository, OrderService orderService) {
        this.repository = repository;
        this.orderService = orderService;
    }

    @GetMapping
    public Page<Order> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size
    ) {
        Sort sort = Sort.by("id").descending();
        return repository.findAll(PageRequest.of(page, size, sort));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Order> getOrderByUuid(@PathVariable UUID uuid) {
        return repository.findByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody @Valid CreateOrderRequest request) {
        Order savedOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<Order> patchOrder(
            @PathVariable UUID uuid,
            @RequestBody PatchOrderRequest patch
    ) {
        return orderService.patchOrder(uuid, patch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
