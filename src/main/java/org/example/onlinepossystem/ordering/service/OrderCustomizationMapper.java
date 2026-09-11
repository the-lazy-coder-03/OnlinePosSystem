package org.example.onlinepossystem.ordering.service;

import org.example.onlinepossystem.catalog.api.OrderCatalogResolver;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderCustomizationMapper {
    public List<OrderCatalogResolver.CatalogCustomizationRequest> toCatalogRequests(
            List<OrderRequestDTO.CustomizationRequestDTO> customizations
    ) {
        if (customizations == null) {
            return List.of();
        }
        return customizations.stream()
                .filter(customization -> customization != null)
                .map(customization -> new OrderCatalogResolver.CatalogCustomizationRequest(
                        customization.getId(),
                        customization.getQuantity(),
                        customization.getType()
                ))
                .toList();
    }
}
