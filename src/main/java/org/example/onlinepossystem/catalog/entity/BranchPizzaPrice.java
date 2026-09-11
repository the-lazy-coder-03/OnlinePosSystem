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
@Table(name = "branch_pizza_price")
public class BranchPizzaPrice {

    @EmbeddedId
    private BranchPizzaPriceId id;

    @ManyToOne
    @MapsId("pizzaId")
    @JoinColumn(name = "pizza_id")
    private Pizza pizza;

    @ManyToOne
    @MapsId("pizzaSizeId")
    @JoinColumn(name = "pizza_size_id")
    private PizzaSize pizzaSize;

    @Column(nullable = false)
    private Double price;

    public BranchPizzaPrice() {}

    public BranchPizzaPrice(Integer branchId, Pizza pizza, PizzaSize pizzaSize, Double price) {
        this.pizza = pizza;
        this.pizzaSize = pizzaSize;
        this.price = price;
        this.id = new BranchPizzaPriceId(branchId, pizza.getId(), pizzaSize.getId());
    }

    public BranchPizzaPriceId getId() { return id; }
    public void setId(BranchPizzaPriceId id) { this.id = id; }
    public Integer getBranchId() { return id == null ? null : id.getBranchId(); }
    public Pizza getPizza() { return pizza; }
    public void setPizza(Pizza pizza) { this.pizza = pizza; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Embeddable
    public static class BranchPizzaPriceId implements Serializable {
        private Integer branchId;
        private Integer pizzaId;
        private Integer pizzaSizeId;

        public BranchPizzaPriceId() {}
        public BranchPizzaPriceId(Integer branchId, Integer pizzaId, Integer pizzaSizeId) {
            this.branchId = branchId;
            this.pizzaId = pizzaId;
            this.pizzaSizeId = pizzaSizeId;
        }

        public Integer getBranchId() { return branchId; }
        public void setBranchId(Integer branchId) { this.branchId = branchId; }
        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        public Integer getPizzaSizeId() { return pizzaSizeId; }
        public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchPizzaPriceId that = (BranchPizzaPriceId) o;
            return Objects.equals(branchId, that.branchId) && 
                   Objects.equals(pizzaId, that.pizzaId) &&
                   Objects.equals(pizzaSizeId, that.pizzaSizeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branchId, pizzaId, pizzaSizeId);
        }
    }
}
