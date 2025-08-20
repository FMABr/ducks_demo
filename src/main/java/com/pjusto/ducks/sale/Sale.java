package com.pjusto.ducks.sale;

import com.pjusto.ducks.customer.Customer;
import com.pjusto.ducks.employee.Employee;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "sale",
        indexes = {
                @Index(name = "idx_sale_employee_date", columnList = "employee_id,sale_date"),
                @Index(name = "idx_sale_customer_date", columnList = "customer_id,sale_date"),
                @Index(name = "idx_sale_date", columnList = "sale_date")
        }
)
@Getter
@Setter
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_before_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBeforeDiscount;

    @NotNull
    @PositiveOrZero
    @Column(name = "total_after_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAfterDiscount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sale_customer")
    )
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sale_employee")
    )
    private Employee employee;

    @NotNull
    @Column(name = "sale_date", nullable = false)
    private Instant saleDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (this.saleDate == null) {
            this.saleDate = Instant.now();
        }
    }
}
