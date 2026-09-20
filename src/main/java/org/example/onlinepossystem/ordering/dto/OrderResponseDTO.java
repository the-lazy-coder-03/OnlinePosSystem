package org.example.onlinepossystem.ordering.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderResponseDTO {
    private Long id;
    private Integer branchId;
    private String branchName;
    private String status;
    private String orderType;
    private LocalDateTime createdAt;
    private String customerName;
    private String phone;
    private String houseNumber;
    private String street;
    private String area;
    private String city;
    private String postalCode;
    private String complexName;
    private String notes;
    private List<MenuItemDTO> menuItems = new ArrayList<>();
    private List<PizzaItemDTO> pizzaItems = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getComplexName() { return complexName; }
    public void setComplexName(String complexName) { this.complexName = complexName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<MenuItemDTO> getMenuItems() { return menuItems; }
    public void setMenuItems(List<MenuItemDTO> menuItems) { this.menuItems = menuItems; }
    public List<PizzaItemDTO> getPizzaItems() { return pizzaItems; }
    public void setPizzaItems(List<PizzaItemDTO> pizzaItems) { this.pizzaItems = pizzaItems; }

    public static class MenuItemDTO {
        private Long id;
        private Integer menuItemId;
        private String menuItemName;
        private Integer qty;
        private Double unitPriceAtTime;
        private String notes;
        private List<MenuItemExtraDTO> extras = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getMenuItemId() { return menuItemId; }
        public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }
        public String getMenuItemName() { return menuItemName; }
        public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }
        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }
        public Double getUnitPriceAtTime() { return unitPriceAtTime; }
        public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<MenuItemExtraDTO> getExtras() { return extras; }
        public void setExtras(List<MenuItemExtraDTO> extras) { this.extras = extras; }
    }

    public static class MenuItemExtraDTO {
        private Long id;
        private String name;
        private Integer qty;
        private Double unitPriceAtTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }
        public Double getUnitPriceAtTime() { return unitPriceAtTime; }
        public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
    }

    public static class PizzaItemDTO {
        private Long id;
        private Integer pizzaId;
        private String pizzaName;
        private Integer pizzaSizeId;
        private Integer pizzaSizeCm;
        private Integer qty;
        private Double basePriceAtTime;
        private Integer pizzaBaseOptionId;
        private String pizzaBaseOptionName;
        private Double pizzaBaseOptionPriceAtTime;
        private String notes;
        private List<PizzaItemExtraDTO> extras = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        public String getPizzaName() { return pizzaName; }
        public void setPizzaName(String pizzaName) { this.pizzaName = pizzaName; }
        public Integer getPizzaSizeId() { return pizzaSizeId; }
        public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }
        public Integer getPizzaSizeCm() { return pizzaSizeCm; }
        public void setPizzaSizeCm(Integer pizzaSizeCm) { this.pizzaSizeCm = pizzaSizeCm; }
        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }
        public Double getBasePriceAtTime() { return basePriceAtTime; }
        public void setBasePriceAtTime(Double basePriceAtTime) { this.basePriceAtTime = basePriceAtTime; }
        public Integer getPizzaBaseOptionId() { return pizzaBaseOptionId; }
        public void setPizzaBaseOptionId(Integer pizzaBaseOptionId) { this.pizzaBaseOptionId = pizzaBaseOptionId; }
        public String getPizzaBaseOptionName() { return pizzaBaseOptionName; }
        public void setPizzaBaseOptionName(String pizzaBaseOptionName) { this.pizzaBaseOptionName = pizzaBaseOptionName; }
        public Double getPizzaBaseOptionPriceAtTime() { return pizzaBaseOptionPriceAtTime; }
        public void setPizzaBaseOptionPriceAtTime(Double pizzaBaseOptionPriceAtTime) { this.pizzaBaseOptionPriceAtTime = pizzaBaseOptionPriceAtTime; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<PizzaItemExtraDTO> getExtras() { return extras; }
        public void setExtras(List<PizzaItemExtraDTO> extras) { this.extras = extras; }
    }

    public static class PizzaItemExtraDTO {
        private Long id;
        private Integer ingredientId;
        private String ingredientName;
        private Integer qty;
        private Double unitPriceAtTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getIngredientId() { return ingredientId; }
        public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }
        public String getIngredientName() { return ingredientName; }
        public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }
        public Double getUnitPriceAtTime() { return unitPriceAtTime; }
        public void setUnitPriceAtTime(Double unitPriceAtTime) { this.unitPriceAtTime = unitPriceAtTime; }
    }
}
