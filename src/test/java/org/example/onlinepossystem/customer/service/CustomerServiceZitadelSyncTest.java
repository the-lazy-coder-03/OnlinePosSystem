package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.security.api.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceZitadelSyncTest {
    private CustomerRepository customerRepository;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);
        customerService = new CustomerService(customerRepository, passwordEncoder, passwordPolicy);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsCustomerForNewVerifiedZitadelIdentity() {
        when(customerRepository.findByZitadelSubject("subject-1")).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

        Customer customer = customerService.syncZitadelCustomer(
                "subject-1",
                " Customer@Example.com ",
                "Ada",
                "Lovelace"
        );

        assertThat(customer.getZitadelSubject()).isEqualTo("subject-1");
        assertThat(customer.getEmail()).isEqualTo("customer@example.com");
        assertThat(customer.getFirstName()).isEqualTo("Ada");
        assertThat(customer.getLastName()).isEqualTo("Lovelace");
        assertThat(customer.getRole()).isEqualTo("USER");
        assertThat(customer.getLastOrderedAt()).isNotNull();
        verify(customerRepository).save(customer);
    }

    @Test
    void replacesEmailForExistingZitadelSubject() {
        Customer existing = new Customer();
        existing.setId(10L);
        existing.setEmail("old@example.com");
        existing.setZitadelSubject("subject-1");
        when(customerRepository.findByZitadelSubject("subject-1")).thenReturn(Optional.of(existing));
        when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        Customer customer = customerService.syncZitadelCustomer(
                "subject-1",
                "new@example.com",
                "Grace",
                "Hopper"
        );

        assertThat(customer).isSameAs(existing);
        assertThat(customer.getEmail()).isEqualTo("new@example.com");
        assertThat(customer.getFirstName()).isEqualTo("Grace");
        assertThat(customer.getLastName()).isEqualTo("Hopper");
    }

    @Test
    void rejectsEmailCollisionForExistingZitadelSubject() {
        Customer linked = new Customer();
        linked.setId(10L);
        linked.setEmail("old@example.com");
        linked.setZitadelSubject("subject-1");

        Customer other = new Customer();
        other.setId(11L);
        other.setEmail("new@example.com");

        when(customerRepository.findByZitadelSubject("subject-1")).thenReturn(Optional.of(linked));
        when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> customerService.syncZitadelCustomer(
                "subject-1",
                "new@example.com",
                "Grace",
                "Hopper"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked");
    }
}
