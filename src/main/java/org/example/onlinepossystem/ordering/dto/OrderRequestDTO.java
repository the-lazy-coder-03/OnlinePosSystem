package org.example.onlinepossystem.ordering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class OrderRequestDTO {
    @NotBlank
    private String customerName;
    private String phone;
    private String houseNumber;
    private String street;
    private String area;
    private String city;
    private String postalCode;
    private String complexName;
    @Size(max = 64, message = "Gate access code must be 64 characters or fewer.")
    @Pattern(regexp = "[A-Za-z0-9 #*]*", message = "Gate access code may contain only letters, numbers, spaces, # and *.")
    private String gateAccessCode;
    @NotBlank
    private String branchName;
    private String orderType; // "pickup" or "delivery"
    @Valid
    private List<OrderItemRequestDTO> items;
    @Valid
    private List<SpecialItemRequestDTO> specialItems;

    // Getters and Setters
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
    public String getGateAccessCode() { return gateAccessCode; }
    public void setGateAccessCode(String gateAccessCode) { this.gateAccessCode = gateAccessCode; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public List<OrderItemRequestDTO> getItems() { return items; }
    public void setItems(List<OrderItemRequestDTO> items) { this.items = items; }
    public List<SpecialItemRequestDTO> getSpecialItems() { return specialItems; }
    public void setSpecialItems(List<SpecialItemRequestDTO> specialItems) { this.specialItems = specialItems; }

    public static class OrderItemRequestDTO {
        private Integer menuItemId;
        private Integer pizzaId;
        private Integer pizzaSizeId;
        private Integer sizeCm;
        private Integer pizzaBaseOptionId;
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
        public Integer getSizeCm() { return sizeCm; }
        public void setSizeCm(Integer sizeCm) { this.sizeCm = sizeCm; }
        public Integer getPizzaBaseOptionId() { return pizzaBaseOptionId; }
        public void setPizzaBaseOptionId(Integer pizzaBaseOptionId) { this.pizzaBaseOptionId = pizzaBaseOptionId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public List<CustomizationRequestDTO> getCustomizations() { return customizations; }
        public void setCustomizations(List<CustomizationRequestDTO> customizations) { this.customizations = customizations; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class SpecialItemRequestDTO {
        @NotNull private Long specialId;
        @NotNull @Positive private Integer quantity;
        @Valid private List<SpecialSelectionRequestDTO> selections;
        @Valid private List<SpecialAddonRequestDTO> addons;
        public Long getSpecialId() { return specialId; }
        public void setSpecialId(Long specialId) { this.specialId = specialId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public List<SpecialSelectionRequestDTO> getSelections() { return selections; }
        public void setSelections(List<SpecialSelectionRequestDTO> selections) { this.selections = selections; }
        public List<SpecialAddonRequestDTO> getAddons() { return addons; }
        public void setAddons(List<SpecialAddonRequestDTO> addons) { this.addons = addons; }
    }

    public static class SpecialSelectionRequestDTO {
        @NotNull private Long componentId;
        @NotNull @Positive private Integer selectionIndex;
        @NotNull @Valid private OrderItemRequestDTO item;
        public Long getComponentId() { return componentId; }
        public void setComponentId(Long componentId) { this.componentId = componentId; }
        public Integer getSelectionIndex() { return selectionIndex; }
        public void setSelectionIndex(Integer selectionIndex) { this.selectionIndex = selectionIndex; }
        public OrderItemRequestDTO getItem() { return item; }
        public void setItem(OrderItemRequestDTO item) { this.item = item; }
    }

    public static class SpecialAddonRequestDTO {
        @NotNull private Long addonId;
        @NotNull @Positive private Integer quantity;
        @NotNull @Valid private OrderItemRequestDTO item;
        public Long getAddonId() { return addonId; }
        public void setAddonId(Long addonId) { this.addonId = addonId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public OrderItemRequestDTO getItem() { return item; }
        public void setItem(OrderItemRequestDTO item) { this.item = item; }
    }

    public static class CustomizationRequestDTO {
        @NotNull
        private Integer id;
        @NotNull
        @Positive
        private Integer quantity;
        private String type;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
