package org.example.onlinepossystem.customer.web;

import jakarta.persistence.EntityManager;
import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.branch.repository.BranchRepository;
import org.example.onlinepossystem.catalog.entity.BranchMenuItemPrice;
import org.example.onlinepossystem.catalog.entity.MenuCategory;
import org.example.onlinepossystem.catalog.entity.MenuItem;
import org.example.onlinepossystem.catalog.repository.BranchMenuItemPriceRepository;
import org.example.onlinepossystem.catalog.repository.MenuCategoryRepository;
import org.example.onlinepossystem.catalog.repository.MenuItemRepository;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.service.CustomerService;
import org.example.onlinepossystem.ordering.api.OrderOperations;
import org.example.onlinepossystem.ordering.dto.OrderRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerProfileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderOperations orderOperations;

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

    @Autowired
    private EntityManager entityManager;

    @Test
    void profileShowsRealLinkedRecentOrdersOnly() throws Exception {
        Customer visibleCustomer = createCustomer("profile-visible");
        Customer hiddenCustomer = createCustomer("profile-hidden");
        MenuFixture visibleFixture = createMenuFixture("Visible Profile Item " + suffix(), 37.50);
        MenuFixture hiddenFixture = createMenuFixture("Hidden Profile Item " + suffix(), 88.00);

        orderOperations.placeOrderForCustomer(
                request(visibleFixture.branch().getName(), visibleFixture.menuItem().getId(), 2),
                visibleCustomer.getEmail()
        );
        orderOperations.placeOrderForCustomer(
                request(hiddenFixture.branch().getName(), hiddenFixture.menuItem().getId(), 1),
                hiddenCustomer.getEmail()
        );
        entityManager.flush();

        mockMvc.perform(get("/profile/edit").with(user(visibleCustomer.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("customerInfoEdit"))
                .andExpect(model().attributeExists("customer", "recentOrders"))
                .andExpect(content().string(containsString(visibleFixture.menuItem().getName())))
                .andExpect(content().string(containsString("R75.00")))
                .andExpect(content().string(not(containsString(hiddenFixture.menuItem().getName()))))
                .andExpect(content().string(not(containsString("Margherita Pizza"))))
                .andExpect(content().string(not(containsString("Pepperoni Pizza"))));
    }

    private Customer createCustomer(String label) {
        String suffix = suffix();
        return customerService.registerCustomer(
                "Profile",
                label,
                label + "-" + suffix + "@example.com",
                "Password1!",
                uniquePhone(),
                null,
                "1",
                "Main Street",
                "Area",
                null,
                "Kenridge Branch",
                "7550"
        );
    }

    private MenuFixture createMenuFixture(String name, double price) {
        String suffix = suffix();
        Branch branch = branchRepository.saveAndFlush(new Branch(
                nextId("branch", "branch_id"),
                "Profile Branch " + suffix
        ));
        MenuCategory category = menuCategoryRepository.saveAndFlush(new MenuCategory(
                nextId("menu_category", "id"),
                "Profile Category " + suffix,
                1
        ));
        MenuItem item = menuItemRepository.saveAndFlush(new MenuItem(
                nextId("menu_item", "id"),
                category,
                name,
                "Profile history fixture",
                1,
                false,
                false
        ));
        branchMenuItemPriceRepository.saveAndFlush(new BranchMenuItemPrice(branch, item, price));
        return new MenuFixture(branch, item);
    }

    private OrderRequestDTO request(String branchName, Integer menuItemId, int quantity) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setCustomerName("Profile Customer");
        request.setPhone("0210000000");
        request.setBranchName(branchName);
        request.setOrderType("pickup");

        OrderRequestDTO.OrderItemRequestDTO item = new OrderRequestDTO.OrderItemRequestDTO();
        item.setMenuItemId(menuItemId);
        item.setQuantity(quantity);
        item.setCustomizations(List.of());
        request.setItems(List.of(item));
        return request;
    }

    private int nextId(String tableName, String columnName) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(" + columnName + "), 0) + 100 FROM " + tableName,
                Integer.class
        );
        return value == null ? 100 : value;
    }

    private String uniquePhone() {
        String raw = Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits());
        return raw.length() > 20 ? raw.substring(0, 20) : raw;
    }

    private String suffix() {
        return UUID.randomUUID().toString();
    }

    private record MenuFixture(Branch branch, MenuItem menuItem) {
    }
}
