package com.pjusto.ducks.sale;

import com.pjusto.ducks.duck.Duck;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "sale_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_saleitem_duck", columnNames = "duck_id")
        }
)
@Getter
@Setter
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @PositiveOrZero
    @Column(name = "price_at_sale", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtSale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sale_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sale_item_sale")
    )
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "duck_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sale_item_duck")
    )
    private Duck duck;
}
