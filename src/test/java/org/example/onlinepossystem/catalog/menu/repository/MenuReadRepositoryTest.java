package org.example.onlinepossystem.catalog.menu.repository;

import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.branch.repository.BranchRepository;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.entity.ModifierGroup;
import org.example.onlinepossystem.catalog.entity.ModifierOption;
import org.example.onlinepossystem.catalog.menu.dto.MenuItemRow;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.MenuCategoryRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.catalog.repository.ModifierGroupRepository;
import org.example.onlinepossystem.catalog.repository.ModifierOptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MenuReadRepositoryTest {

    @Autowired
    private MenuReadRepository menuReadRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private BranchMenuItemPriceRepository branchMenuItemPriceRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private ModifierOptionRepository modifierOptionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsOnlyItemsPricedForTheSelectedBranch() {
        String suffix = UUID.randomUUID().toString();
        int branchId = nextId("branch", "branch_id");
        int categoryId = nextId("menu_category", "id");
        int firstItemId = nextId("menu_item", "id");

        Branch branch = branchRepository.saveAndFlush(new Branch(branchId, "Menu branch " + suffix));
        MenuCategory category = menuCategoryRepository.saveAndFlush(
                new MenuCategory(categoryId, "Menu category " + suffix, 1)
        );
        MenuItem available = menuItemRepository.saveAndFlush(new MenuItem(
                firstItemId,
                category,
                "Available item " + suffix,
                "Available at this branch",
                1,
                false,
                false
        ));
        MenuItem unavailable = menuItemRepository.saveAndFlush(new MenuItem(
                firstItemId + 1,
                category,
                "Unavailable item " + suffix,
                "No branch price",
                2,
                false,
                false
        ));
        branchMenuItemPriceRepository.saveAndFlush(new BranchMenuItemPrice(branch.getId(), available, 75.00));

        List<MenuItemRow> rows = menuReadRepository.findMenuItemsForBranch(branch.getId());

        assertThat(rows).extracting(MenuItemRow::menuItemId)
                .contains(available.getId())
                .doesNotContain(unavailable.getId());
    }

    @Test
    void ordersDrinkOptionsFromLowestToHighestExtraCost() {
        modifierGroupRepository.saveAndFlush(new ModifierGroup(1, "Choose your drink", true, 1, 1));

        int optionId = nextId("modifier_option", "id");
        saveDrinkOption(optionId, "Premium drink", "4.00");
        saveDrinkOption(optionId + 1, "Included drink B", "0.00");
        saveDrinkOption(optionId + 2, "Mid-price drink", "2.00");
        saveDrinkOption(optionId + 3, "Included drink A", "0.00");
        saveDrinkOption(optionId + 4, "Low-cost drink", "1.00");

        assertThat(menuReadRepository.findModifierOptionsForGroups(List.of(1)))
                .extracting(row -> row.name())
                .containsExactly(
                        "Included drink A",
                        "Included drink B",
                        "Low-cost drink",
                        "Mid-price drink",
                        "Premium drink"
                );
    }

    private void saveDrinkOption(int id, String name, String price) {
        ModifierOption option = new ModifierOption(id, 1, name, null);
        option.setAdditionalPrice(new BigDecimal(price));
        modifierOptionRepository.saveAndFlush(option);
    }

    private int nextId(String tableName, String columnName) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(" + columnName + "), 0) + 100 FROM " + tableName,
                Integer.class
        );
        return value == null ? 100 : value;
    }
}
