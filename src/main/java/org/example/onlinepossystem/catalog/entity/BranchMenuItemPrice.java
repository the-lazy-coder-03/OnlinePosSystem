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
@Table(name = "branch_menu_item_price")
public class BranchMenuItemPrice {

    @EmbeddedId
    private BranchMenuItemPriceId id;

    @ManyToOne
    @MapsId("branchId")
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne
    @MapsId("menuItemId")
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Column(nullable = false)
    private Double price;

    public BranchMenuItemPrice() {}

    public BranchMenuItemPrice(Branch branch, MenuItem menuItem, Double price) {
        this.branch = branch;
        this.menuItem = menuItem;
        this.price = price;
        this.id = new BranchMenuItemPriceId(branch.getId(), menuItem.getId());
    }

    public BranchMenuItemPriceId getId() { return id; }
    public void setId(BranchMenuItemPriceId id) { this.id = id; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Embeddable
    public static class BranchMenuItemPriceId implements Serializable {
        private Integer branchId;
        private Integer menuItemId;

        public BranchMenuItemPriceId() {}
        public BranchMenuItemPriceId(Integer branchId, Integer menuItemId) {
            this.branchId = branchId;
            this.menuItemId = menuItemId;
        }

        public Integer getBranchId() { return branchId; }
        public void setBranchId(Integer branchId) { this.branchId = branchId; }
        public Integer getMenuItemId() { return menuItemId; }
        public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchMenuItemPriceId that = (BranchMenuItemPriceId) o;
            return Objects.equals(branchId, that.branchId) && 
                   Objects.equals(menuItemId, that.menuItemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branchId, menuItemId);
        }
    }
}
