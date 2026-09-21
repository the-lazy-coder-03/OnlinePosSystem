package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerAccessServiceTest {

    @Test
    void assignsEveryLevelAndKeepsTheLegacyRoleSynchronized() {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer customer = new Customer();
        customer.setId(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(customer));
        CustomerAccessService service = new CustomerAccessService(repository, "environment-admin");
        String[] expectedRoles = {"USER", "ADMIN", "ADMIN", "SUPER_ADMIN", "DRIVER"};

        for (int level = 0; level <= 4; level++) {
            service.assignAccessLevel(42L, level);
            assertThat(customer.getAccessLevel()).isEqualTo(level);
            assertThat(customer.getRole()).isEqualTo(expectedRoles[level]);
        }

        verify(repository, org.mockito.Mockito.times(5)).save(customer);
    }

    @Test
    void environmentAdminIsAlwaysLevelThreeAndInvalidAssignmentsFailClosed() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerAccessService service = new CustomerAccessService(repository, "environment-admin");

        assertThat(service.findByUsername("environment-admin").level()).isEqualTo(3);
        assertThatThrownBy(() -> service.assignAccessLevel(1L, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
