package com.pjusto.ducks.reporting;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "v_sold_duck")
@Immutable
@Getter @Setter
public class SoldDuckView {
    @Id
    @Column(name = "duck_id")
    private Long duckId;

    @Column(name = "duck_name", nullable = false)
    private String duckName;

    @Column(name = "price_at_sale", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtSale;

    @Column(name = "sale_id", nullable = false)   private Long saleId;
    @Column(name = "sale_date", nullable = false) private Instant saleDate;

    @Column(name = "customer_id", nullable = false)   private Long customerId;
    @Column(name = "customer_name", nullable = false) private String customerName;

    @Column(name = "employee_id", nullable = false)   private Long employeeId;
    @Column(name = "employee_name", nullable = false) private String employeeName;
}
