package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.branch.api.BranchView;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.MenuItemModifierGroup;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.MenuCategoryRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.catalog.repository.ModifierGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
public class MenuCatalogAdminService {
    private static final Logger logger = LoggerFactory.getLogger(MenuCatalogAdminService.class);

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final BranchLookup branchLookup;
    private final BranchMenuItemPriceRepository branchMenuItemPriceRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    public MenuCatalogAdminService(
            MenuItemRepository menuItemRepository,
            MenuCategoryRepository menuCategoryRepository,
            BranchLookup branchLookup,
            BranchMenuItemPriceRepository branchMenuItemPriceRepository,
            ModifierGroupRepository modifierGroupRepository,
            MenuItemModifierGroupRepository menuItemModifierGroupRepository
    ) {
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.branchLookup = branchLookup;
        this.branchMenuItemPriceRepository = branchMenuItemPriceRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.menuItemModifierGroupRepository = menuItemModifierGroupRepository;
    }

    @Transactional
    public void saveMenuItem(Integer id, String name, Integer categoryId, String description, Integer sortOrder,
                             List<Integer> modifierGroupIds, Map<String, String> parameters, String actor) {
        MenuItem menuItem = id == null ? new MenuItem() : menuItemRepository.findById(id).orElseGet(MenuItem::new);
        if (menuItem.getId() == null) {
            menuItem.setId(nextId(menuItemRepository.findAll(), MenuItem::getId));
        }
        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Menu category not found with ID: " + categoryId));
        menuItem.setCategory(category);
        menuItem.setName(cleanText(name));
        menuItem.setDescription(cleanText(description));
        menuItem.setSortOrder(sortOrder == null ? 0 : sortOrder);
        menuItem.setActive(parameters.containsKey("active"));
        menuItem.setIs300ml(parameters.containsKey("is300ml"));
        menuItem.setIs2l(parameters.containsKey("is2l"));

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        replaceModifierGroups(savedMenuItem, modifierGroupIds);
        savePriceMatrix(savedMenuItem, parameters);
        logger.info("Admin action=saveMenuItem menuItemId={} admin={}", savedMenuItem.getId(), actorName(actor));
    }

    public void deleteMenuItem(Integer id, String actor) {
        menuItemRepository.deleteById(id);
        logger.info("Admin action=deleteMenuItem menuItemId={} admin={}", id, actorName(actor));
    }

    @Transactional
    public void updateMenuItemPrice(Integer branchId, Integer menuItemId, Double price, String actor) {
        branchLookup.requireById(branchId);
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Menu item not found with ID: " + menuItemId));
        savePrice(branchId, menuItem, price);
        logger.info("Admin action=updateMenuItemPrice branchId={} menuItemId={} admin={}",
                branchId, menuItemId, actorName(actor));
    }

    private void replaceModifierGroups(MenuItem menuItem, List<Integer> modifierGroupIds) {
        menuItemModifierGroupRepository.deleteAll(menuItemModifierGroupRepository.findByMenuItemId(menuItem.getId()));
        if (modifierGroupIds == null) {
            return;
        }
        for (Integer groupId : modifierGroupIds) {
            if (modifierGroupRepository.existsById(groupId)) {
                menuItemModifierGroupRepository.save(new MenuItemModifierGroup(menuItem.getId(), groupId));
            }
        }
    }

    private void savePriceMatrix(MenuItem menuItem, Map<String, String> parameters) {
        for (BranchView branch : branchLookup.findAll()) {
            parsePrice(parameters.get("menuPrice_" + branch.id()))
                    .ifPresent(price -> savePrice(branch.id(), menuItem, price));
        }
    }

    private void savePrice(Integer branchId, MenuItem menuItem, Double price) {
        BranchMenuItemPrice branchPrice = branchMenuItemPriceRepository
                .findByBranchIdAndMenuItemId(branchId, menuItem.getId())
                .orElse(new BranchMenuItemPrice(branchId, menuItem, price));
        branchPrice.setPrice(price);
        branchMenuItemPriceRepository.save(branchPrice);
    }

    private <T> Integer nextId(List<T> items, Function<T, Integer> idExtractor) {
        return items.stream().map(idExtractor).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private Optional<Double> parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        double price = Double.parseDouble(raw);
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        return Optional.of(price);
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private String actorName(String actor) {
        return actor == null || actor.isBlank() ? "unknown" : actor;
    }
}
