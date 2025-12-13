package pl.monmat.manager.api;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.monmat.manager.api.dto.CreateOrderRequest;
import pl.monmat.manager.api.service.OrderService;

import java.util.Optional;

@RestController
public class OrderController {
    private final OrderRepository repository;
    private final OrderService orderService;

    public OrderController(OrderRepository repository, OrderService orderService) {
        this.repository = repository;
        this.orderService = orderService;
    }

    @GetMapping("/api/orders/{page}")
    public Page<Order> getAll(@PathVariable Integer page) {
        Sort sort = Sort.by("id").descending();
        return repository.findAll(PageRequest.of(page, 100, sort));
    }

    @GetMapping("/api/order/{id}")
    public Optional<Order> getById(@PathVariable Long id) {
        return repository.findById(id);
    }

    @PostMapping("/api/order")
    public ResponseEntity<Order> create(@RequestBody @Valid CreateOrderRequest request) {

        Order savedOrder = orderService.createOrder(request);

        // return 201 - created status
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }


}
