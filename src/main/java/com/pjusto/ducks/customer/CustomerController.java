package com.pjusto.ducks.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerUpsertRequest req) {
        Customer customer = new Customer();
        customer.setName(req.name());
        customer.setHasSalesDiscount(req.hasSalesDiscount());

        Customer saved = customerRepository.save(customer);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        return toResponse(customer);
    }

    @PutMapping("/{id}")
    public CustomerResponse replace(@PathVariable Long id, @Valid @RequestBody CustomerUpsertRequest req) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        customer.setName(req.name());
        customer.setHasSalesDiscount(req.hasSalesDiscount());

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public CustomerResponse patch(@PathVariable Long id, @RequestBody CustomerPatchRequest req) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        if (StringUtils.hasText(req.name())) {
            customer.setName(req.name());
        }
        if (req.hasSalesDiscount() != null) {
            customer.setHasSalesDiscount(req.hasSalesDiscount());
        }

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        customerRepository.delete(customer);
    }

    @GetMapping
    public Page<CustomerResponse> list(
            @RequestParam(required = false) @Nullable String name,
            @RequestParam(required = false) @Nullable Boolean salesDiscount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Customer probe = new Customer();
        if (StringUtils.hasText(name)) {
            probe.setName(name);
        }
        if (salesDiscount != null) {
            probe.setHasSalesDiscount(salesDiscount);
        }

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnorePaths("id", "createdAt", "updatedAt")
                .withMatcher("name", m -> m.contains().ignoreCase());

        Page<Customer> customers = customerRepository.findAll(Example.of(probe, matcher), PageRequest.of(page, size));
        return customers.map(CustomerController::toResponse);
    }

    private static CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getHasSalesDiscount(), c.getCreatedAt(), c.getUpdatedAt());
    }

    public record CustomerUpsertRequest(@NotBlank String name, @NotNull Boolean hasSalesDiscount) {
    }

    public record CustomerPatchRequest(String name, Boolean hasSalesDiscount) {
    }

    public record CustomerResponse(Long id, String name, Boolean hasSalesDiscount, Instant createdAt, Instant updatedAt) {
    }
}
