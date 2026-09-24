package org.example.onlinepossystem.shared.web;

import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MainControllerCheckoutTest {

    @Test
    void checkoutLoadsTheSignedInCustomersSavedDetails() {
        CustomerAccountReader customerReader = mock(CustomerAccountReader.class);
        AccountAccessReader accessReader = mock(AccountAccessReader.class);
        CustomerAccount customer = new CustomerAccount(
                42L, "0821234567", null, "jamie@example.com", "12", "Main Road", "Kenridge",
                "Oak Mews", null, "Kenridge", "Cape Town", "7550", "Jamie", "Smith", "USER", 1
        );
        when(customerReader.findByEmail("jamie@example.com")).thenReturn(Optional.of(customer));
        MainController controller = new MainController(customerReader, accessReader);
        ExtendedModelMap model = new ExtendedModelMap();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "jamie@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String view = controller.checkoutPage(model, authentication);

        assertThat(view).isEqualTo("checkout");
        assertThat(model.get("customerName")).isEqualTo("Jamie Smith");
        assertThat(model.get("user")).isSameAs(customer);
    }
}
