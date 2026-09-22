package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.entity.CustomerNote;
import org.example.onlinepossystem.customer.repository.CustomerNoteRepository;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCustomerRecordServiceTest {
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CustomerNoteRepository notes = mock(CustomerNoteRepository.class);
    private final AdminCustomerRecordService service = new AdminCustomerRecordService(customers, notes);
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(7L);
        customer.setFirstName("Ada");
        customer.setLastName("Smith");
        customer.setEmail("ada@example.test");
        customer.setPhone1("0110000000");
        customer.setPhone2("0820000000");
        customer.setHouseNumber("3");
        customer.setStreet("Main Road");
        customer.setArea("Kenridge");
        customer.setComplexName("Court");
        customer.setCity("Cape Town");
        customer.setPostalCode("7530");
        customer.setPreferredStore("Kenridge");
        customer.setAccessLevel(4);
        customer.setPassword("never-serialize-this");
        when(customers.findById(7L)).thenReturn(Optional.of(customer));
    }

    @Test
    void searchIsDatabasePagedAndDetailExcludesSecrets() {
        when(customers.searchForAdmin(eq("ada smith"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        var page = service.search(" ADA Smith ", 0);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).accessLabel()).isEqualTo("Delivery driver");
        assertThat(service.detail(7L).phone2()).isEqualTo("0820000000");
        assertThat(service.detail(7L).street()).isEqualTo("Main Road");
        assertThat(java.util.Arrays.stream(service.detail(7L).getClass().getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("password", "passwordHash", "resetToken");
        verify(customers).searchForAdmin(eq("ada smith"), any(Pageable.class));
    }

    @Test
    void rejectsInvalidPagesAndMissingCustomers() {
        assertThatThrownBy(() -> service.search("", -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.detail(99L)).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void notesAreTrimmedSavedWithAuthorAndCannotBeBlankOrOversized() {
        when(notes.saveAndFlush(any(CustomerNote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var saved = service.addNote(7L, "  Called about order  ", "admin@example.test");
        assertThat(saved.body()).isEqualTo("Called about order");
        assertThat(saved.authorUsername()).isEqualTo("admin@example.test");
        assertThatThrownBy(() -> service.addNote(7L, "  ", "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.addNote(7L, "x".repeat(2001), "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
