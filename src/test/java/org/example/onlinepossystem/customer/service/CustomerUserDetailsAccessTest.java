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

    @ParameterizedTest
    @CsvSource({
            "0,ROLE_USER",
            "1,ROLE_ADMIN",
            "2,ROLE_ADMIN",
            "3,ROLE_ADMIN|ROLE_SUPER_ADMIN",
            "4,ROLE_DRIVER"
    })
    void derivesAuthoritiesFromAccessLevel(int accessLevel, String expectedAuthorities) {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer customer = new Customer();
        customer.setEmail("person@example.com");
        customer.setPassword("password-hash");
        customer.setAccessLevel(accessLevel);
        when(repository.findByEmail("person@example.com")).thenReturn(Optional.of(customer));
        CustomerUserDetailsService service = new CustomerUserDetailsService(new org.example.onlinepossystem.customer.persistence.JpaAccountBootstrapStore(repository, null), "admin", "admin-password");

        UserDetails details = service.loadUserByUsername("person@example.com");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly(expectedAuthorities.split("\\|"));
    }
}
