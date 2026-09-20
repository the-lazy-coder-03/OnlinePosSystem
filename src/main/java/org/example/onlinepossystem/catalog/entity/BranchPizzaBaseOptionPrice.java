package org.example.onlinepossystem.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "branch_pizza_base_option_price")
public class BranchPizzaBaseOptionPrice {
    @EmbeddedId
    private BranchPizzaBaseOptionPriceId id;

    @ManyToOne
    @MapsId("pizzaBaseOptionId")
    @JoinColumn(name = "pizza_base_option_id")
    private PizzaBaseOption pizzaBaseOption;

    @ManyToOne
    @MapsId("pizzaSizeId")
    @JoinColumn(name = "pizza_size_id")
    private PizzaSize pizzaSize;

    @Column(nullable = false)
    private Double price;

    public BranchPizzaBaseOptionPrice() {}

    public BranchPizzaBaseOptionPrice(
            Integer branchId,
            PizzaBaseOption pizzaBaseOption,
            PizzaSize pizzaSize,
            Double price
    ) {
        this.id = new BranchPizzaBaseOptionPriceId(branchId, pizzaBaseOption.getId(), pizzaSize.getId());
        this.pizzaBaseOption = pizzaBaseOption;
        this.pizzaSize = pizzaSize;
        this.price = price;
    }

    public BranchPizzaBaseOptionPriceId getId() { return id; }
    public void setId(BranchPizzaBaseOptionPriceId id) { this.id = id; }
    public Integer getBranchId() { return id == null ? null : id.getBranchId(); }
    public PizzaBaseOption getPizzaBaseOption() { return pizzaBaseOption; }
    public void setPizzaBaseOption(PizzaBaseOption pizzaBaseOption) { this.pizzaBaseOption = pizzaBaseOption; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Embeddable
    public static class BranchPizzaBaseOptionPriceId implements Serializable {
        private Integer branchId;
        private Integer pizzaBaseOptionId;
        private Integer pizzaSizeId;

        public BranchPizzaBaseOptionPriceId() {}

        public BranchPizzaBaseOptionPriceId(Integer branchId, Integer pizzaBaseOptionId, Integer pizzaSizeId) {
            this.branchId = branchId;
            this.pizzaBaseOptionId = pizzaBaseOptionId;
            this.pizzaSizeId = pizzaSizeId;
        }

        public Integer getBranchId() { return branchId; }
        public void setBranchId(Integer branchId) { this.branchId = branchId; }
        public Integer getPizzaBaseOptionId() { return pizzaBaseOptionId; }
        public void setPizzaBaseOptionId(Integer pizzaBaseOptionId) { this.pizzaBaseOptionId = pizzaBaseOptionId; }
        public Integer getPizzaSizeId() { return pizzaSizeId; }
        public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchPizzaBaseOptionPriceId that = (BranchPizzaBaseOptionPriceId) o;
            return Objects.equals(branchId, that.branchId)
                    && Objects.equals(pizzaBaseOptionId, that.pizzaBaseOptionId)
                    && Objects.equals(pizzaSizeId, that.pizzaSizeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branchId, pizzaBaseOptionId, pizzaSizeId);
        }
    }
}
