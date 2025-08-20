package com.pjusto.ducks.duck;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(indexes = @Index(name = "idx_duck_mother", columnList = "mother_id"))
@Getter
@Setter
@org.hibernate.annotations.Check(constraints = "mother_id IS NULL OR mother_id <> id")
public class Duck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Formula("""
      (SELECT CASE
          WHEN COUNT(*) = 0 THEN 70.00
          WHEN COUNT(*) = 1 THEN 50.00
          ELSE 25.00
        END
       FROM duck d2
       WHERE d2.mother_id = id)
      """)
    @PositiveOrZero
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "mother_id", foreignKey = @ForeignKey(name = "fk_duck_mother"))
    private Duck mother;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
