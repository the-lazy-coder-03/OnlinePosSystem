package org.example.onlinepossystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "burger_toppings")
public class BurgerTopping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "burger_id", nullable = false)
    private MenuItem burger;

    @Column(name = "topping_name", nullable = false)
    private String toppingName;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(nullable = false)
    private Double price;

    public BurgerTopping() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MenuItem getBurger() { return burger; }
    public void setBurger(MenuItem burger) { this.burger = burger; }
    public String getToppingName() { return toppingName; }
    public void setToppingName(String toppingName) { this.toppingName = toppingName; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
