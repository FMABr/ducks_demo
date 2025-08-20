package com.pjusto.ducks.duck;

import com.pjusto.ducks.sale.SaleItem;
import com.pjusto.ducks.sale.SaleItemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.*;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

@RestController
@RequestMapping("/ducks")
public class DuckController {
    private final DuckRepository duckRepository;
    private final SaleItemRepository saleItemRepository;

    public DuckController(DuckRepository duckRepository, SaleItemRepository saleItemRepository) {
        this.duckRepository = duckRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @PostMapping
    public ResponseEntity<DuckWithPriceResponse> create(@Valid @RequestBody DuckUpsertRequest req) {
        Duck duck = new Duck();
        duck.setName(req.name());
        duck.setMother(resolveMother(req.motherId(), null));

        Duck saved = duckRepository.save(duck);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @GetMapping("/{id}")
    public DuckWithPriceResponse getById(@PathVariable Long id) {
        Duck duck = duckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Duck not found"));
        return toResponse(duck);
    }

    @PutMapping("/{id}")
    public DuckWithPriceResponse replace(@PathVariable Long id, @Valid @RequestBody DuckUpsertRequest req) {
        Duck duck = duckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Duck not found"));

        duck.setName(req.name());
        duck.setMother(resolveMother(req.motherId(), id));

        Duck saved = duckRepository.save(duck);
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public DuckWithPriceResponse patch(@PathVariable Long id, @RequestBody DuckPatchRequest req) {
        Duck duck = duckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Duck not found"));

        if (StringUtils.hasText(req.name())) {
            duck.setName(req.name());
        }
        if (req.motherId() != null) {
            duck.setMother(resolveMother(req.motherId(), id));
        }

        Duck saved = duckRepository.save(duck);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Duck duck = duckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Duck not found"));

        duckRepository.delete(duck);
    }

    @GetMapping
    public Page<DuckWithPriceResponse> list(
            @RequestParam(required = false) @Nullable String name,
            @RequestParam(required = false) @Nullable Long motherId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<DuckRepository.DuckWithPrice> p = duckRepository.searchWithPrice(
                (name == null || name.isBlank()) ? null : "%"+name.toLowerCase()+"%",
                motherId,
                PageRequest.of(page, size)
        );

        return p.map(row -> new DuckWithPriceResponse(
                row.getId(),
                row.getName(),
                row.getPrice(),
                row.getMotherId(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        ));
    }

    @GetMapping("/sold")
    @Transactional(readOnly = true)
    public Page<SoldDuckResponse> listSold(
            @RequestParam(required = false) @Nullable String from,
            @RequestParam(required = false) @Nullable String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LocalDate fromDate = null;
        LocalDate toDate = null;

        if (StringUtils.hasText(from)) {
            parseDateOrBadRequest(from);
            fromDate = LocalDate.parse(from);
        }
        if (StringUtils.hasText(to)) {
            parseDateOrBadRequest(to);
            toDate = LocalDate.parse(to);
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`from` must be <= `to`");
        }

        Instant start = (fromDate != null)
                ? fromDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;

        Instant endExclusive = (toDate != null)
                ? toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("sale.saleDate"), Sort.Order.desc("id"))
        );

        Page<SaleItem> items;
        if (start != null && endExclusive != null) {
            items = saleItemRepository
                    .findBySale_SaleDateGreaterThanEqualAndSale_SaleDateLessThan(start, endExclusive, pageable);
        } else if (start != null) {
            items = saleItemRepository.findBySale_SaleDateGreaterThanEqual(start, pageable);
        } else if (endExclusive != null) {
            items = saleItemRepository.findBySale_SaleDateLessThan(endExclusive, pageable);
        } else {
            items = saleItemRepository.findAll(pageable);
        }

        return items.map(si -> new SoldDuckResponse(
                si.getDuck().getId(),
                si.getDuck().getName(),
                si.getSale().getCustomer().getName(),
                si.getSale().getSaleDate(),
                si.getPriceAtSale()
        ));
    }

    private Duck resolveMother(Long motherId, Long selfId) {
        if (motherId == null) return null;
        if (Objects.equals(motherId, selfId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A duck cannot be its own mother");
        }
        return duckRepository.findById(motherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mother duck not found: id=" + motherId));
    }

    private static void parseDateOrBadRequest(String s) {
        try {
            LocalDate.parse(s);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date, expected YYYY-MM-DD: " + s);
        }
    }

    private static DuckWithPriceResponse toResponse(Duck d) {
        Long motherId = (d.getMother() != null) ? d.getMother().getId() : null;
        return new DuckWithPriceResponse(
                d.getId(),
                d.getName(),
                d.getPrice(),
                motherId,
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    public record DuckUpsertRequest(
            @NotBlank String name,
            Long motherId
    ) {
    }

    public record DuckPatchRequest(
            String name,
            Long motherId
    ) {
    }

    public record DuckWithPriceResponse(
            Long id,
            String name,
            BigDecimal price,
            Long motherId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SoldDuckResponse(
            Long duckId,
            String duckName,
            String customerName,
            Instant saleDate,
            BigDecimal priceAtSale
    ) {}
}
