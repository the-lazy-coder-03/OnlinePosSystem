package org.example.onlinepossystem.dto;

import java.util.List;

public class MenuDTO {
    private String categoryName;
    private Integer menuItemId;
    private String menuItemName;
    private String description;
    private Double price;
    private boolean is300ml;
    private boolean is2l;
    private boolean isPizza;
    private Integer pizzaSizeId;
    private List<CustomizationDTO> customizations;

    public static class CustomizationDTO {
        private Integer id;
        private String name;
        private Double price;
        private boolean isDefault;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean aDefault) { isDefault = aDefault; }
    }

    // Getters and Setters
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }
    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public boolean isIs300ml() { return is300ml; }
    public void setIs300ml(boolean is300ml) { this.is300ml = is300ml; }
    public boolean isIs2l() { return is2l; }
    public void setIs2l(boolean is2l) { this.is2l = is2l; }
    public boolean isPizza() { return isPizza; }
    public void setPizza(boolean pizza) { isPizza = pizza; }
    public Integer getPizzaSizeId() { return pizzaSizeId; }
    public void setPizzaSizeId(Integer pizzaSizeId) { this.pizzaSizeId = pizzaSizeId; }
    public List<CustomizationDTO> getCustomizations() { return customizations; }
    public void setCustomizations(List<CustomizationDTO> customizations) { this.customizations = customizations; }
}
