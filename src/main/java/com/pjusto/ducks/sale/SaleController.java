package com.pjusto.ducks.sale;

import com.pjusto.ducks.customer.Customer;
import com.pjusto.ducks.customer.CustomerRepository;
import com.pjusto.ducks.duck.Duck;
import com.pjusto.ducks.duck.DuckRepository;
import com.pjusto.ducks.employee.Employee;
import com.pjusto.ducks.employee.EmployeeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sales")
public class SaleController {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final DuckRepository duckRepository;

    public SaleController(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            DuckRepository duckRepository
    ) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.duckRepository = duckRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleCreateRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Customer not found: id=" + req.customerId()));

        Employee employee = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Employee not found: id=" + req.employeeId()));

        Set<Long> uniqueDuckIds = new LinkedHashSet<>(req.duckIds());
        if (uniqueDuckIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duckIds must be non-empty");
        }

        List<Duck> ducks = duckRepository.findAllById(uniqueDuckIds);
        if (ducks.size() != uniqueDuckIds.size()) {
            Set<Long> found = ducks.stream().map(Duck::getId).collect(Collectors.toSet());
            List<Long> missing = uniqueDuckIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Some ducks were not found: " + missing);
        }

        var existing = saleItemRepository.findByDuck_IdIn(uniqueDuckIds);
        if (!existing.isEmpty()) {
            List<Long> soldDuckIds = existing.stream()
                    .map(si -> si.getDuck().getId())
                    .distinct()
                    .toList();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Some ducks are already sold: " + soldDuckIds);
        }

        BigDecimal totalBefore = ducks.stream()
                .map(Duck::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasDiscount = customer.getHasSalesDiscount();
        BigDecimal factor = hasDiscount ? new BigDecimal("0.80") : BigDecimal.ONE;
        List<BigDecimal> itemPrices = ducks.stream()
                .map(d -> d.getPrice().multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP))
                .toList();

        BigDecimal totalAfter = itemPrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setEmployee(employee);
        sale.setTotalBeforeDiscount(totalBefore);
        sale.setTotalAfterDiscount(totalAfter);
        sale.setSaleDate(Instant.now());

        try {
            Sale saved = saleRepository.save(sale);

            List<SaleItem> items = new ArrayList<>(ducks.size());
            for (int i = 0; i < ducks.size(); i++) {
                Duck duck = ducks.get(i);
                SaleItem si = new SaleItem();
                si.setSale(saved);
                si.setDuck(duck);
                si.setPriceAtSale(itemPrices.get(i));
                items.add(si);
            }

            saleItemRepository.saveAll(items);
            saleItemRepository.flush();

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri();

            return ResponseEntity.created(location).body(toResponse(saved));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Some ducks are already sold",
                    ex
            );
        }
    }


    @GetMapping("/{id}")
    public SaleResponse getById(@PathVariable Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale not found"));
        return toResponse(sale);
    }

    @GetMapping
    public Page<SaleResponse> list(
            @RequestParam(required = false) @Nullable String from,
            @RequestParam(required = false) @Nullable String to,
            @RequestParam(required = false) @Nullable Long customerId,
            @RequestParam(required = false) @Nullable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant fromInstant = null;
        Instant toInstantExclusive = null;

        if (StringUtils.hasText(from)) {
            fromInstant = parseDateOrBadRequest(from).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        if (StringUtils.hasText(to)) {
            toInstantExclusive = parseDateOrBadRequest(to).plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        Specification<Sale> spec = Specification.where(null);

        if (fromInstant != null) {
            Instant ff = fromInstant;
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("saleDate"), ff));
        }
        if (toInstantExclusive != null) {
            Instant tt = toInstantExclusive;
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("saleDate"), tt));
        }
        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }
        if (employeeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }

        Page<Sale> pageResult = saleRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "saleDate", "id"))
        );

        return pageResult.map(SaleController::toResponse);
    }

    private static SaleResponse toResponse(Sale s) {
        Long customerId = (s.getCustomer() != null) ? s.getCustomer().getId() : null;
        Long employeeId = (s.getEmployee() != null) ? s.getEmployee().getId() : null;

        return new SaleResponse(
                s.getId(),
                s.getTotalBeforeDiscount(),
                s.getTotalAfterDiscount(),
                customerId,
                employeeId,
                s.getSaleDate(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private static LocalDate parseDateOrBadRequest(String s) {
        try {
            return LocalDate.parse(s);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date, expected YYYY-MM-DD: " + s
            );
        }
    }


    public record SaleCreateRequest(
            @NotNull Long customerId,
            @NotNull Long employeeId,
            @NotEmpty List<Long> duckIds
    ) {
    }

    public record SaleResponse(
            Long id,
            BigDecimal totalBeforeDiscount,
            BigDecimal totalAfterDiscount,
            Long customerId,
            Long employeeId,
            Instant saleDate,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
