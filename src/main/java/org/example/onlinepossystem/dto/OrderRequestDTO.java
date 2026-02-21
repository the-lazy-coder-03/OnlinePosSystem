package org.example.onlinepossystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class OrderRequestDTO {
    @NotBlank
    private String customerName;
    private String phone;
    @NotBlank
    private String branchName;
    private String orderType; // "pickup" or "delivery"
    @NotNull
    @Size(min = 1)
    @Valid
    private List<OrderItemRequestDTO> items;

    // Getters and Setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public List<OrderItemRequestDTO> getItems() { return items; }
    public void setItems(List<OrderItemRequestDTO> items) { this.items = items; }

    public static class OrderItemRequestDTO {
        private Integer menuItemId;
        private Integer pizzaId;
        private Integer pizzaSizeId;
        @NotNull
        @Positive
        private Integer quantity;
        @Valid
        private List<CustomizationRequestDTO> customizations;
        private String notes;

        // Getters and Setters
        public Integer getMenuItemId() { return menuItemId; }
        public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }
        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        public Integer getPizzaSizeId() { return pizzaSizeId; }
        public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public List<CustomizationRequestDTO> getCustomizations() { return customizations; }
        public void setCustomizations(List<CustomizationRequestDTO> customizations) { this.customizations = customizations; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class CustomizationRequestDTO {
        @NotNull
        private Integer id;
        @NotNull
        @Positive
        private Integer quantity;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
