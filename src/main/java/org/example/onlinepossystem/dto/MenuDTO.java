package org.example.onlinepossystem.dto;

import java.util.List;

public class MenuDTO {
    private String categoryName;
    private Long menuItemId;
    private String menuItemName;
    private String description;
    private List<SizePriceDTO> availableSizes;
    private List<ToppingDTO> availableToppings;

    public static class SizePriceDTO {
        private Integer sizeId;
        private Integer cm;
        private Double price;

        // Getters and Setters
        public Integer getSizeId() { return sizeId; }
        public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }
        public Integer getCm() { return cm; }
        public void setCm(Integer cm) { this.cm = cm; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }

    public static class ToppingDTO {
        private Integer id;
        private String name;
        private List<SizePriceDTO> extraPrices; // Price depends on pizza size

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<SizePriceDTO> getExtraPrices() { return extraPrices; }
        public void setExtraPrices(List<SizePriceDTO> extraPrices) { this.extraPrices = extraPrices; }
    }

    // Getters and Setters
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }
    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<SizePriceDTO> getAvailableSizes() { return availableSizes; }
    public void setAvailableSizes(List<SizePriceDTO> availableSizes) { this.availableSizes = availableSizes; }
    public List<ToppingDTO> getAvailableToppings() { return availableToppings; }
    public void setAvailableToppings(List<ToppingDTO> availableToppings) { this.availableToppings = availableToppings; }
}
