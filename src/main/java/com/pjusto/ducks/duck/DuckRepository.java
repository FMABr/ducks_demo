package com.pjusto.ducks.duck;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface DuckRepository extends JpaRepository<Duck, Long>, JpaSpecificationExecutor<Duck> {

    @Query("""
        select d.id as id,
               d.name as name,
               d.mother.id as motherId,
               (select count(c) from Duck c where c.mother.id = d.id) as childCount,
               d.createdAt as createdAt,
               d.updatedAt as updatedAt
        from Duck d
        where (:name is null or lower(d.name) like lower(concat('%', :name, '%')))
          and (:motherId is null or d.mother.id = :motherId)
        """)
    Page<DuckWithChildCount> searchWithChildCount(
            @Param("name") String name,
            @Param("motherId") Long motherId,
            Pageable pageable
    );

    @Query("""
        select d.id as id,
               d.name as name,
               d.mother.id as motherId,
               (select count(c) from Duck c where c.mother.id = d.id) as childCount,
               d.createdAt as createdAt,
               d.updatedAt as updatedAt
        from Duck d
        where d.id = :id
        """)
    Optional<DuckWithChildCount> findOneWithChildCount(@Param("id") Long id);


    @Query("""
  select d.id as id,
         d.name as name,
         d.mother.id as motherId,
         (select count(c) from Duck c where c.mother.id = d.id) as childCount,
         case
           when (select count(c1) from Duck c1 where c1.mother.id = d.id) = 0 then 70.00
           when (select count(c2) from Duck c2 where c2.mother.id = d.id) = 1 then 50.00
           else 25.00
         end as price,
         d.createdAt as createdAt,
         d.updatedAt as updatedAt
  from Duck d
  where (:name is null or lower(d.name) like :name)
    and (:motherId is null or d.mother.id = :motherId)
  """)
    Page<DuckWithPrice> searchWithPrice(@Param("name") String name,
                                        @Param("motherId") Long motherId,
                                        Pageable pageable);

    interface DuckWithChildCount {
        Long getId();
        String getName();
        Long getMotherId();
        Long getChildCount();
        Instant getCreatedAt();
        Instant getUpdatedAt();
    }


    interface DuckWithPrice {
        Long getId();
        String getName();
        Long getMotherId();
        Long getChildCount();
        java.math.BigDecimal getPrice();
        java.time.Instant getCreatedAt();
        java.time.Instant getUpdatedAt();
    }

}