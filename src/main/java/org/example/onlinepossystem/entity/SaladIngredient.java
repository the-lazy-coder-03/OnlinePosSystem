package org.example.onlinepossystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "salad_ingredients")
public class SaladIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "salad_id", nullable = false)
    private MenuItem salad;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(nullable = false)
    private Double price;

    public SaladIngredient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MenuItem getSalad() { return salad; }
    public void setSalad(MenuItem salad) { this.salad = salad; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
