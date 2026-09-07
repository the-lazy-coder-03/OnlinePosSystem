package org.example.onlinepossystem.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.example.onlinepossystem.branch.entity.Branch;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "branch_extra_price")
public class BranchExtraPrice {

    @EmbeddedId
    private BranchExtraPriceId id;

    @ManyToOne
    @MapsId("branchId")
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne
    @MapsId("priceCategoryId")
    @JoinColumn(name = "price_category_id")
    private PriceCategory priceCategory;

    @ManyToOne
    @MapsId("pizzaSizeId")
    @JoinColumn(name = "pizza_size_id")
    private PizzaSize pizzaSize;

    @Column(nullable = false)
    private Double price;

    public BranchExtraPrice() {}

    public BranchExtraPrice(Branch branch, PriceCategory priceCategory, PizzaSize pizzaSize, Double price) {
        this.branch = branch;
        this.priceCategory = priceCategory;
        this.pizzaSize = pizzaSize;
        this.price = price;
        this.id = new BranchExtraPriceId(branch.getId(), priceCategory.getId(), pizzaSize.getId());
    }

    public BranchExtraPriceId getId() { return id; }
    public void setId(BranchExtraPriceId id) { this.id = id; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public PriceCategory getPriceCategory() { return priceCategory; }
    public void setPriceCategory(PriceCategory priceCategory) { this.priceCategory = priceCategory; }
    public PizzaSize getPizzaSize() { return pizzaSize; }
    public void setPizzaSize(PizzaSize pizzaSize) { this.pizzaSize = pizzaSize; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Embeddable
    public static class BranchExtraPriceId implements Serializable {
        private Integer branchId;
        private Integer priceCategoryId;
        private Integer pizzaSizeId;

        public BranchExtraPriceId() {}
        public BranchExtraPriceId(Integer branchId, Integer priceCategoryId, Integer pizzaSizeId) {
            this.branchId = branchId;
            this.priceCategoryId = priceCategoryId;
            this.pizzaSizeId = pizzaSizeId;
        }

        public Integer getBranchId() { return branchId; }
        public void setBranchId(Integer branchId) { this.branchId = branchId; }
        public Integer getPriceCategoryId() { return priceCategoryId; }
        public void setPriceCategoryId(Integer priceCategoryId) { this.priceCategoryId = priceCategoryId; }
        public Integer getPizzaSizeId() { return pizzaSizeId; }
        public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchExtraPriceId that = (BranchExtraPriceId) o;
            return Objects.equals(branchId, that.branchId) && Objects.equals(priceCategoryId, that.priceCategoryId) && Objects.equals(pizzaSizeId, that.pizzaSizeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branchId, priceCategoryId, pizzaSizeId);
        }
    }
}
