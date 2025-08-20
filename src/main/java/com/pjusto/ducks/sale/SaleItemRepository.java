package com.pjusto.ducks.sale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findByDuck_IdIn(Collection<Long> duckIds);

    Page<SaleItem> findBySale_SaleDateGreaterThanEqualAndSale_SaleDateLessThan(
            Instant fromInclusive,
            Instant toExclusive,
            Pageable pageable
    );

    Page<SaleItem> findBySale_SaleDateGreaterThanEqual(Instant fromInclusive, Pageable pageable);

    Page<SaleItem> findBySale_SaleDateLessThan(Instant toExclusive, Pageable pageable);
}
