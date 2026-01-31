package org.example.onlinepossystem.dto;

import java.util.List;

public class OrderRequestDTO {
    private String customerName;
    private String phone;
    private String branchName;
    private String orderType; // "pickup" or "delivery"
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
        private Integer pizzaId;
        private Integer pizzaSizeId;
        private Integer quantity;
        private List<ExtraRequestDTO> extras;
        private String notes;

        // Getters and Setters
        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        public Integer getPizzaSizeId() { return pizzaSizeId; }
        public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public List<ExtraRequestDTO> getExtras() { return extras; }
        public void setExtras(List<ExtraRequestDTO> extras) { this.extras = extras; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class ExtraRequestDTO {
        private Integer ingredientId;
        private Integer quantity;

        // Getters and Setters
        public Integer getIngredientId() { return ingredientId; }
        public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
