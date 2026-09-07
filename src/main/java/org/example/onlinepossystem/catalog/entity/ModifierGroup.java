package org.example.onlinepossystem.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "modifier_group")
public class ModifierGroup {
    @Id
    @Column(name = "id")
    public Integer id;

    @Column(name = "name")
    public String name;

    @Column(name = "required")
    public Boolean required;

    @Column(name = "min_select")
    public Integer minSelect;

    @Column(name = "max_select")
    public Integer maxSelect;

    public ModifierGroup() {
    }

    public ModifierGroup(Integer id, String name, Boolean required, Integer minSelect, Integer maxSelect) {
        this.id = id;
        this.name = name;
        this.required = required;
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Integer getMinSelect() { return minSelect; }
    public void setMinSelect(Integer minSelect) { this.minSelect = minSelect; }
    public Integer getMaxSelect() { return maxSelect; }
    public void setMaxSelect(Integer maxSelect) { this.maxSelect = maxSelect; }
}
