package org.example.onlinepossystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "burger_component")
public class BurgerComponent {
    @Id
    @Column(name = "component_id")
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "component_type", nullable = false)
    private String componentType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean seasonal = false;

    public BurgerComponent() {
    }

    public BurgerComponent(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSeasonal() {
        return seasonal;
    }

    public void setSeasonal(boolean seasonal) {
        this.seasonal = seasonal;
    }
}
