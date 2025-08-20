package com.pjusto.ducks.employee;

import com.pjusto.ducks.sale.SaleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final SaleRepository saleRepository;

    public EmployeeController(
            EmployeeRepository employeeRepository,
            SaleRepository saleRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.saleRepository = saleRepository;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeUpsertRequest req) {
        Employee employee = new Employee();
        employee.setName(req.name());
        employee.setCpf(req.cpf());
        employee.setEmployee_code(req.employeeCode());

        Employee saved = employeeRepository.save(employee);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        return toResponse(employee);
    }

    @PutMapping("/{id}")
    public EmployeeResponse replace(@PathVariable Long id, @Valid @RequestBody EmployeeUpsertRequest req) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        employee.setName(req.name());
        employee.setCpf(req.cpf());
        employee.setEmployee_code(req.employeeCode());

        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @PatchMapping("/{id}")
    public EmployeeResponse patch(@PathVariable Long id, @RequestBody EmployeePatchRequest req) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (StringUtils.hasText(req.name())) {
            employee.setName(req.name());
        }
        if (StringUtils.hasText(req.cpf())) {
            employee.setCpf(req.cpf());
        }
        if (StringUtils.hasText(req.employeeCode())) {
            employee.setEmployee_code(req.employeeCode());
        }

        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (saleRepository.existsByEmployee_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee cannot be deleted because they have recorded sales"
            );
        }

        try {
            employeeRepository.delete(employee);
            employeeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee cannot be deleted because they have recorded sales",
                    ex
            );
        }
    }

    @GetMapping
    public Page<EmployeeResponse> list(
            @RequestParam(required = false) @Nullable String name,
            @RequestParam(required = false) @Nullable String cpf,
            @RequestParam(required = false) @Nullable String employeeCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Employee probe = new Employee();
        if (StringUtils.hasText(name)) {
            probe.setName(name);
        }
        if (StringUtils.hasText(cpf)) {
            probe.setCpf(cpf);
        }
        if (StringUtils.hasText(employeeCode)) {
            probe.setEmployee_code(employeeCode);
        }

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnorePaths("id", "createdAt", "updatedAt")
                .withMatcher("name", m -> m.contains().ignoreCase())
                .withMatcher("cpf", ExampleMatcher.GenericPropertyMatcher::exact)
                .withMatcher("employee_code", ExampleMatcher.GenericPropertyMatcher::exact);

        Page<Employee> employees = employeeRepository.findAll(
                Example.of(probe, matcher),
                PageRequest.of(page, size)
        );

        return employees.map(EmployeeController::toResponse);
    }

    private static EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getName(),
                e.getCpf(),
                e.getEmployee_code(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public record EmployeeUpsertRequest(
            @NotBlank String name,
            @NotBlank String cpf,
            @NotBlank String employeeCode
    ) {
    }

    public record EmployeePatchRequest(
            String name,
            String cpf,
            String employeeCode
    ) {
    }

    public record EmployeeResponse(
            Long id,
            String name,
            String cpf,
            String employeeCode,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
