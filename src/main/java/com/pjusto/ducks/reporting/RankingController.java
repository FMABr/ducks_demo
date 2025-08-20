package com.pjusto.ducks.reporting;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/employees/rankings")
public class RankingController {

    private final EmployeeRankingRepository rankRepository;

    public RankingController(EmployeeRankingRepository rankRepository) {
        this.rankRepository = rankRepository;
    }

    @GetMapping("/count")
    public List<EmployeeRankingItem> rankByCount(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (limit <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be > 0");
        Bounds b = parseBounds(from, to);
        var pr = PageRequest.of(0, limit, Sort.unsorted());
        List<EmployeeRankingView> rows = rankRepository.rankByCount(b.from, b.toExclusive, pr);
        return toItems(rows);
    }

    @GetMapping("/revenue")
    public List<EmployeeRankingItem> rankByRevenue(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (limit <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be > 0");
        Bounds b = parseBounds(from, to);
        var pr = PageRequest.of(0, limit, Sort.unsorted());
        List<EmployeeRankingView> rows = rankRepository.rankByRevenue(b.from, b.toExclusive, pr);
        return toItems(rows);
    }

    private static Bounds parseBounds(String from, String to) {
        Instant f, tEx;
        if (from == null || from.isBlank()) {
            f = Instant.parse("0001-01-01T00:00:00Z");
        } else {
            f = parseDateOrBadRequest(from).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        if (to == null || to.isBlank()) {
            tEx = Instant.parse("9999-12-31T23:59:59Z");
        } else {
            tEx = parseDateOrBadRequest(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return new Bounds(f, tEx);
    }


    private static List<EmployeeRankingItem> toItems(List<EmployeeRankingView> rows) {
        List<EmployeeRankingItem> out = new ArrayList<>(rows.size());
        int rank = 1;
        for (EmployeeRankingView r : rows) {
            out.add(new EmployeeRankingItem(
                    rank++,
                    r.getEmployeeId(),
                    r.getEmployeeName(),
                    r.getSaleCount(),
                    r.getRevenue()
            ));
        }
        return out;
    }

    private static LocalDate parseDateOrBadRequest(String s) {
        try {
            return LocalDate.parse(s);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date, expected YYYY-MM-DD: " + s);
        }
    }

    private record Bounds(Instant from, Instant toExclusive) {
    }

    public record EmployeeRankingItem(
            int rank,
            Long employeeId,
            String employeeName,
            Long saleCount,
            BigDecimal revenue
    ) {
    }
}
