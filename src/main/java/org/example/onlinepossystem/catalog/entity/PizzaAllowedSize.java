package org.example.onlinepossystem.catalog.entity;

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
@Table(name = "pizza_allowed_size")
public class PizzaAllowedSize {

    @EmbeddedId
    private PizzaAllowedSizeId id;

    @ManyToOne
    @MapsId("pizzaId")
    @JoinColumn(name = "pizza_id")
    private Pizza pizza;

    @ManyToOne
    @MapsId("pizzaSizeId")
    @JoinColumn(name = "pizza_size_id")
    private PizzaSize pizzaSize;

    public PizzaAllowedSize() {}

    public PizzaAllowedSize(Pizza pizza, PizzaSize pizzaSize) {
        this.pizza = pizza;
        this.pizzaSize = pizzaSize;
        this.id = new PizzaAllowedSizeId(pizza.getId(), pizzaSize.getId());
    }

    public PizzaAllowedSizeId getId() { return id; }
    public void setId(PizzaAllowedSizeId id) { this.id = id; }
    public Pizza getPizza() { return pizza; }
    public void setPizza(Pizza pizza) { this.pizza = pizza; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }

    @Embeddable
    public static class PizzaAllowedSizeId implements Serializable {
        private Integer pizzaId;
        private Integer pizzaSizeId;

        public PizzaAllowedSizeId() {}
        public PizzaAllowedSizeId(Integer pizzaId, Integer pizzaSizeId) {
            this.pizzaId = pizzaId;
            this.pizzaSizeId = pizzaSizeId;
        }

        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        public Integer getPizzaSizeId() { return pizzaSizeId; }
        public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PizzaAllowedSizeId that = (PizzaAllowedSizeId) o;
            return Objects.equals(pizzaId, that.pizzaId) && Objects.equals(pizzaSizeId, that.pizzaSizeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pizzaId, pizzaSizeId);
        }
    }
}
