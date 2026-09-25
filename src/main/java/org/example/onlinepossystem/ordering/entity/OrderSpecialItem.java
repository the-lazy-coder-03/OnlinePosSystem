package org.example.onlinepossystem.ordering.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_special_item")
public class OrderSpecialItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_special_item_id") private Long id;
    @ManyToOne @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(name = "special_id") private Long specialId;
    @Column(name = "special_name_at_time", nullable = false) private String specialNameAtTime;
    @Column(name = "special_description_at_time") private String specialDescriptionAtTime;
    @Column(nullable = false) private Integer quantity = 1;
    @Column(name = "base_price_at_time", nullable = false, precision = 10, scale = 2) private BigDecimal basePriceAtTime;
    @Column(name = "customization_total_at_time", nullable = false, precision = 10, scale = 2) private BigDecimal customizationTotalAtTime = BigDecimal.ZERO;
    @Column(name = "addon_total_at_time", nullable = false, precision = 10, scale = 2) private BigDecimal addonTotalAtTime = BigDecimal.ZERO;
    @Column(name = "final_line_total_at_time", nullable = false, precision = 10, scale = 2) private BigDecimal finalLineTotalAtTime;
    @OneToMany(mappedBy = "orderSpecialItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("selectionKind, selectionIndex, id") private List<OrderSpecialSelection> selections = new ArrayList<>();

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Long getSpecialId() { return specialId; }
    public void setSpecialId(Long specialId) { this.specialId = specialId; }
    public String getSpecialNameAtTime() { return specialNameAtTime; }
    public void setSpecialNameAtTime(String value) { specialNameAtTime = value; }
    public String getSpecialDescriptionAtTime() { return specialDescriptionAtTime; }
    public void setSpecialDescriptionAtTime(String value) { specialDescriptionAtTime = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getBasePriceAtTime() { return basePriceAtTime; }
    public void setBasePriceAtTime(BigDecimal value) { basePriceAtTime = value; }
    public BigDecimal getCustomizationTotalAtTime() { return customizationTotalAtTime; }
    public void setCustomizationTotalAtTime(BigDecimal value) { customizationTotalAtTime = value; }
    public BigDecimal getAddonTotalAtTime() { return addonTotalAtTime; }
    public void setAddonTotalAtTime(BigDecimal value) { addonTotalAtTime = value; }
    public BigDecimal getFinalLineTotalAtTime() { return finalLineTotalAtTime; }
    public void setFinalLineTotalAtTime(BigDecimal value) { finalLineTotalAtTime = value; }
    public List<OrderSpecialSelection> getSelections() { return selections; }
    public void addSelection(OrderSpecialSelection selection) { selections.add(selection); selection.setOrderSpecialItem(this); }
}
