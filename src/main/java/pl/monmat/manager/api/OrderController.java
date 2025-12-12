package pl.monmat.manager.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class OrderController {
    private final OrderRepository repository;

    public OrderController(OrderRepository repository) {
        this.repository = repository;
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



}
