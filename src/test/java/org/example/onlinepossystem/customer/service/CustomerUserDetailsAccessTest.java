package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerUserDetailsAccessTest {

    @org.junit.jupiter.api.Test
    void environmentSuperAdminSupportsBcryptWithoutChangingItsIdentity() {
        var encoder = org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var stored = encoder.encode("admin");
        var repository = mock(CustomerRepository.class);
        var service = new CustomerUserDetailsService(
                new org.example.onlinepossystem.customer.persistence.JpaAccountBootstrapStore(repository, null), "admin", stored);
        var details = (org.example.onlinepossystem.security.api.AccountPrincipal) service.loadUserByUsername("admin");
        assertThat(details.getPassword()).isEqualTo(stored).startsWith("{bcrypt}");
        assertThat(encoder.matches("admin", details.getPassword())).isTrue();
        assertThat(encoder.matches("incorrect", details.getPassword())).isFalse();
        assertThat(details.environmentAdmin()).isTrue();
        assertThat(details.customerId()).isNull();
        assertThat(details.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @org.junit.jupiter.api.Test
    void existingPlainTextEnvironmentPasswordsRemainCompatible() {
        var encoder = org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var service = new CustomerUserDetailsService(
                new org.example.onlinepossystem.customer.persistence.JpaAccountBootstrapStore(mock(CustomerRepository.class), null),
                "admin", "legacy-password");
        assertThat(encoder.matches("legacy-password", service.loadUserByUsername("admin").getPassword())).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "0,ROLE_USER",
            "1,ROLE_ADMIN",
            "2,ROLE_ADMIN",
            "3,ROLE_ADMIN|ROLE_SUPER_ADMIN|ROLE_USER",
            "4,ROLE_DRIVER"
    })
    void derivesAuthoritiesFromAccessLevel(int accessLevel, String expectedAuthorities) {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer customer = new Customer();
        customer.setId(42L);
        customer.setEmail("person@example.com");
        customer.setPassword("password-hash");
        customer.setAccessLevel(accessLevel);
        when(repository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(customer));
        CustomerUserDetailsService service = new CustomerUserDetailsService(new org.example.onlinepossystem.customer.persistence.JpaAccountBootstrapStore(repository, null), "admin", "admin-password");

        UserDetails details = service.loadUserByUsername("person@example.com");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly(expectedAuthorities.split("\\|"));
    }
}
