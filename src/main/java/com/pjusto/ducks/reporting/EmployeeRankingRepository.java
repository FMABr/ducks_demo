package com.pjusto.ducks.reporting;

import com.pjusto.ducks.sale.Sale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;

public interface EmployeeRankingRepository extends Repository<Sale, Long> {
    @Query("""
  select e.id as employeeId, e.name as employeeName,
         count(s.id) as saleCount,
         coalesce(sum(s.totalAfterDiscount), 0) as revenue
  from Sale s join s.employee e
  where s.saleDate >= :from and s.saleDate < :to
  group by e.id, e.name
  order by count(s.id) desc, coalesce(sum(s.totalAfterDiscount),0) desc, e.id asc
  """)
    List<EmployeeRankingView> rankByCount(Instant from, Instant to, Pageable pageable);

    @Query("""
  select e.id as employeeId, e.name as employeeName,
         count(s.id) as saleCount,
         coalesce(sum(s.totalAfterDiscount), 0) as revenue
  from Sale s join s.employee e
  where s.saleDate >= :from and s.saleDate < :to
  group by e.id, e.name
  order by coalesce(sum(s.totalAfterDiscount),0) desc, count(s.id) desc, e.id asc
  """)
    List<EmployeeRankingView> rankByRevenue(Instant from, Instant to, Pageable pageable);

}
