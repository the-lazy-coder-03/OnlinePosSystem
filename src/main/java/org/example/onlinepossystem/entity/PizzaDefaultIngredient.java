package org.example.onlinepossystem.entity;

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
@Table(name = "pizza_default_ingredient")
public class PizzaDefaultIngredient {

    @EmbeddedId
    private PizzaDefaultIngredientId id;

    @ManyToOne
    @MapsId("pizzaId")
    @JoinColumn(name = "pizza_id")
    private Pizza pizza;

    @ManyToOne
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(name = "is_removable", nullable = false)
    private boolean isRemovable = true;

    @Column(name = "default_qty", nullable = false)
    private Integer defaultQty = 1;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public PizzaDefaultIngredient() {}

    public PizzaDefaultIngredient(Pizza pizza, Ingredient ingredient, boolean isRemovable, Integer defaultQty, Integer sortOrder) {
        this.pizza = pizza;
        this.ingredient = ingredient;
        this.isRemovable = isRemovable;
        this.defaultQty = defaultQty;
        this.sortOrder = sortOrder;
        this.id = new PizzaDefaultIngredientId(pizza.getId(), ingredient.getId());
    }

    public PizzaDefaultIngredientId getId() { return id; }
    public void setId(PizzaDefaultIngredientId id) { this.id = id; }
    public Pizza getPizza() { return pizza; }
    public void setPizza(Pizza pizza) { this.pizza = pizza; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public boolean isRemovable() { return isRemovable; }
    public void setRemovable(boolean removable) { isRemovable = removable; }
    public Integer getDefaultQty() { return defaultQty; }
    public void setDefaultQty(Integer defaultQty) { this.defaultQty = defaultQty; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    @Embeddable
    public static class PizzaDefaultIngredientId implements Serializable {
        private Integer pizzaId;
        private Integer ingredientId;

        public PizzaDefaultIngredientId() {}
        public PizzaDefaultIngredientId(Integer pizzaId, Integer ingredientId) {
            this.pizzaId = pizzaId;
            this.ingredientId = ingredientId;
        }

        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        public Integer getIngredientId() { return ingredientId; }
        public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PizzaDefaultIngredientId that = (PizzaDefaultIngredientId) o;
            return Objects.equals(pizzaId, that.pizzaId) && Objects.equals(ingredientId, that.ingredientId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pizzaId, ingredientId);
        }
    }
}
