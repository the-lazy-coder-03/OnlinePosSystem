package org.example.onlinepossystem.entity;

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

    protected ModifierGroup() {
    }
}
