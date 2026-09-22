package org.example.onlinepossystem.customer.web;

import org.example.onlinepossystem.branch.entity.Branch;
import org.example.onlinepossystem.branch.repository.BranchRepository;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.ordering.entity.Order;
import org.example.onlinepossystem.ordering.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCustomerControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired OrderRepository orders;
    @Autowired BranchRepository branches;

    @Test
    void searchDetailAndBranchFilteredHistoryNeverExposePasswords() throws Exception {
        Customer target = customer(0, "Alice", "alice-" + UUID.randomUUID() + "@example.test");
        target.setPassword("sensitive-hash");
        target.setPhone2("0821234567");
        target.setStreet("Market Street");
        customers.saveAndFlush(target);
        Customer kenridgeAdmin = customer(1, "Kenridge", "kenridge-" + UUID.randomUUID() + "@example.test");
        customers.saveAndFlush(kenridgeAdmin);
        branches.saveAndFlush(new Branch(1, "Kenridge"));
        branches.saveAndFlush(new Branch(2, "Uitzicht"));
        order(target.getId(), 1);
        order(target.getId(), 2);

        mvc.perform(get("/api/admin/customers").param("query", target.getEmail())
                        .with(user(kenridgeAdmin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].email").value(target.getEmail()))
                .andExpect(jsonPath("$.items[0].password").doesNotExist())
                .andExpect(jsonPath("$.pageSize").value(25));
        mvc.perform(get("/api/admin/customers/{id}", target.getId())
                        .with(user(kenridgeAdmin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone2").value("0821234567"))
                .andExpect(jsonPath("$.street").value("Market Street"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/admin/customers/{id}/orders", target.getId())
                        .with(user(kenridgeAdmin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].order.branchId").value(1));
        mvc.perform(get("/api/admin/customers/{id}/orders", target.getId())
                        .with(user("admin").roles("ADMIN", "SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void notesAreAppendOnlyValidatedAndCsrfProtected() throws Exception {
        Customer target = customers.saveAndFlush(customer(0, "Bob", "bob-" + UUID.randomUUID() + "@example.test"));
        Customer branchAdmin = customers.saveAndFlush(customer(2, "Uitzicht", "uitzicht-" + UUID.randomUUID() + "@example.test"));
        String url = "/admin/customers/" + target.getId() + "/notes";
        mvc.perform(post(url).with(user(branchAdmin.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hello\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(url).with(user(branchAdmin.getEmail()).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"  <script>alert(1)</script>  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("<script>alert(1)</script>"))
                .andExpect(jsonPath("$.authorUsername").value(branchAdmin.getEmail()))
                .andExpect(jsonPath("$.createdAt").exists());
        mvc.perform(get("/api/admin/customers/{id}/notes", target.getId())
                        .with(user("admin").roles("ADMIN", "SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].body").value("<script>alert(1)</script>"));
        mvc.perform(post(url).with(user(branchAdmin.getEmail()).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(url).with(user(branchAdmin.getEmail()).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + "x".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersAndDriversCannotReadRecordsOrAddNotes() throws Exception {
        Customer target = customers.saveAndFlush(customer(0, "Carl", "carl-" + UUID.randomUUID() + "@example.test"));
        for (String role : new String[]{"USER", "DRIVER"}) {
            mvc.perform(get("/api/admin/customers").with(user("someone").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/admin/customers/{id}/notes", target.getId())
                            .with(user("someone").roles(role)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"No\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void searchReturnsStableServerPagesAndEmptyHistory() throws Exception {
        String prefix = "batch-" + UUID.randomUUID() + "-";
        for (int i = 0; i < 26; i++) {
            customers.save(customer(0, "Batch", prefix + i + "@example.test"));
        }
        customers.flush();
        mvc.perform(get("/api/admin/customers").param("query", prefix).param("page", "0")
                        .with(user("admin").roles("ADMIN", "SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(25)))
                .andExpect(jsonPath("$.totalItems").value(26))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/admin/customers").param("query", prefix).param("page", "1")
                        .with(user("admin").roles("ADMIN", "SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));
        Long noOrderCustomer = customers.findByEmail(prefix + "0@example.test").orElseThrow().getId();
        mvc.perform(get("/api/admin/customers/{id}/orders", noOrderCustomer)
                        .with(user("admin").roles("ADMIN", "SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    private Customer customer(int accessLevel, String firstName, String email) {
        Customer value = new Customer();
        value.setFirstName(firstName);
        value.setLastName("Example");
        value.setEmail(email);
        value.setPhone1("0" + Math.abs(email.hashCode()));
        value.setAccessLevel(accessLevel);
        value.setRole(accessLevel == 0 ? "USER" : "ADMIN");
        return value;
    }

    private void order(Long customerId, int branchId) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setBranchId(branchId);
        order.setStatus("Completed");
        orders.saveAndFlush(order);
    }
}
