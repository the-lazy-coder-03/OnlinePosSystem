package org.example.onlinepossystem.admin.web;

import org.example.onlinepossystem.catalog.api.CatalogAdministration;
import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminControllerBranchAccessTest {

    @Test
    void branchAdminCannotChangeAnotherBranchesPrice() {
        CatalogAdministration catalog = mock(CatalogAdministration.class);
        AccountAccessReader accessReader = mock(AccountAccessReader.class);
        when(accessReader.findByUsername("kenridge-admin")).thenReturn(new AccountAccess(1));
        AdminController controller = new AdminController(catalog, accessReader);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("kenridge-admin", "n/a", List.of());

        assertThatThrownBy(() -> controller.updatePizzaPrice(2, 10, 3, 99.0, authentication))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(catalog);
    }
}
