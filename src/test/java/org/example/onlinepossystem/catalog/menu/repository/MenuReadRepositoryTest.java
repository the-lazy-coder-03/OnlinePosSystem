package org.example.onlinepossystem.catalog.menu.repository;

import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.branch.repository.BranchRepository;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.menu.dto.MenuItemRow;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.MenuCategoryRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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

    private int nextId(String tableName, String columnName) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(" + columnName + "), 0) + 100 FROM " + tableName,
                Integer.class
        );
        return value == null ? 100 : value;
    }
}
