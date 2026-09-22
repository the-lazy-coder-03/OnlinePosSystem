package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.security.DefaultPasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAccountCreationServiceTest {
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AdminAccountCreationService service =
            new AdminAccountCreationService(customers, encoder, new DefaultPasswordPolicy());

    @Test
    void createsAdminWithOnlyNameEmailPasswordAndLevel() {
        when(customers.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(42L);
            return customer;
        });

        Long id = service.create(" Ada ", " Smith ", " ADA@EXAMPLE.COM ", "Strong1!", 2);

        assertThat(id).isEqualTo(42L);
        org.mockito.ArgumentCaptor<Customer> saved = org.mockito.ArgumentCaptor.forClass(Customer.class);
        verify(customers).saveAndFlush(saved.capture());
        Customer account = saved.getValue();
        assertThat(account.getFirstName()).isEqualTo("Ada");
        assertThat(account.getLastName()).isEqualTo("Smith");
        assertThat(account.getEmail()).isEqualTo("ada@example.com");
        assertThat(encoder.matches("Strong1!", account.getPassword())).isTrue();
        assertThat(account.getAccessLevel()).isEqualTo(2);
        assertThat(account.getRole()).isEqualTo("ADMIN");
        assertThat(account.getPhone1()).isNull();
        assertThat(account.getPhone2()).isNull();
        assertThat(account.getStreet()).isNull();
        assertThat(account.getArea()).isNull();
        assertThat(account.getHouseNumber()).isNull();
    }

    @Test
    void rejectsDuplicateEmailWeakPasswordAndNonAdminLevel() {
        when(customers.existsByEmailIgnoreCase("used@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Ada", "Smith", "used@example.com", "Strong1!", 1))
                .isInstanceOf(CustomerRegistrationException.class)
                .hasMessageContaining("already registered");
        assertThatThrownBy(() -> service.create("Ada", "Smith", "ada@example.com", "weak", 1))
                .isInstanceOf(CustomerRegistrationException.class);
        assertThatThrownBy(() -> service.create("Ada", "Smith", "ada@example.com", "Strong1!", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("Ada", "Smith", "ada@example.com", "Strong1!", 4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(" ", "Smith", "ada@example.com", "Strong1!", 1))
                .isInstanceOf(IllegalArgumentException.class);
        verify(customers, never()).saveAndFlush(any(Customer.class));
    }
}
