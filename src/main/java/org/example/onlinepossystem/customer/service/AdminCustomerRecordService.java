package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.AdminCustomerNoteView;
import org.example.onlinepossystem.customer.api.AdminCustomerView;
import org.example.onlinepossystem.customer.api.CustomerRecordLookup;
import org.example.onlinepossystem.customer.api.PagedResult;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.CustomerNote;
import org.example.onlinepossystem.customer.repository.CustomerNoteRepository;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class AdminCustomerRecordService implements CustomerRecordLookup {
    private static final int PAGE_SIZE = 25;
    private final CustomerRepository customers;
    private final CustomerNoteRepository notes;

    public AdminCustomerRecordService(CustomerRepository customers, CustomerNoteRepository notes) {
        this.customers = customers;
        this.notes = notes;
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminCustomerView> search(String query, int page) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return PagedResult.from(customers.searchForAdmin(needle,
                PageRequest.of(checkedPage(page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"))).map(this::view));
    }

    @Transactional(readOnly = true)
    public AdminCustomerView detail(Long id) {
        return view(findCustomer(id));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireCustomer(Long customerId) {
        findCustomer(customerId);
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminCustomerNoteView> notes(Long customerId, int page) {
        requireCustomer(customerId);
        return PagedResult.from(notes.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId,
                PageRequest.of(checkedPage(page), PAGE_SIZE)).map(this::noteView));
    }

    @Transactional
    public AdminCustomerNoteView addNote(Long customerId, String body, String authorUsername) {
        requireCustomer(customerId);
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty() || trimmed.length() > 2000) {
            throw new IllegalArgumentException("Note must contain 1 to 2000 characters.");
        }
        CustomerNote note = new CustomerNote();
        note.setCustomerId(customerId);
        note.setBody(trimmed);
        note.setAuthorUsername(authorUsername);
        return noteView(notes.saveAndFlush(note));
    }

    private Customer findCustomer(Long id) {
        return customers.findById(id)
                .filter(customer -> !customer.isEnvironmentAdmin())
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + id));
    }

    private int checkedPage(int page) {
        if (page < 0) throw new IllegalArgumentException("Page must be non-negative.");
        return page;
    }

    private AdminCustomerView view(Customer c) {
        int level = c.getAccessLevel() == null ? 0 : c.getAccessLevel();
        String label = switch (level) {
            case 1 -> "Kenridge admin";
            case 2 -> "Uitzicht admin";
            case 3 -> "Super admin";
            case 4 -> "Delivery driver";
            default -> "Normal user";
        };
        return new AdminCustomerView(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(),
                c.getPhone1(), c.getPhone2(), c.getHouseNumber(), c.getStreet(), c.getArea(),
                c.getComplexName(), c.getCity(), c.getPostalCode(), c.getPreferredStore(),
                c.getLastOrderedAt(), level, label);
    }

    private AdminCustomerNoteView noteView(CustomerNote note) {
        return new AdminCustomerNoteView(note.getId(), note.getCustomerId(), note.getAuthorUsername(),
                note.getBody(), note.getCreatedAt());
    }
}
