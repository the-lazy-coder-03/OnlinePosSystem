package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZitadelOidcUserServiceTest {
    private RecordingCustomerSynchronizer customerSynchronizer;
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private OidcUser delegateUser;
    private ZitadelOidcUserService userService;

    @BeforeEach
    void setUp() {
        customerSynchronizer = new RecordingCustomerSynchronizer();
        delegate = ignored -> delegateUser;
        userService = new ZitadelOidcUserService(
                customerSynchronizer,
                "urn:zitadel:iam:org:project:roles",
                delegate
        );
    }

    @Test
    void verifiedZitadelEmailSyncsCustomerAndAuthenticatesAsEmail() {
        OidcUser oidcUser = oidcUser(Map.of(
                "sub", "subject-1",
                "email", "customer@example.com",
                "email_verified", true,
                "given_name", "Ada",
                "family_name", "Lovelace"
        ));
        customerSynchronizer.customer.setRole("USER");
        delegateUser = oidcUser;

        OidcUser result = userService.loadUser(null);

        assertThat(result.getName()).isEqualTo("customer@example.com");
        assertThat(authorityNames(result)).contains("ROLE_USER");
        assertThat(customerSynchronizer.calls).isEqualTo(1);
        assertThat(customerSynchronizer.zitadelSubject).isEqualTo("subject-1");
        assertThat(customerSynchronizer.email).isEqualTo("customer@example.com");
        assertThat(customerSynchronizer.firstName).isEqualTo("Ada");
        assertThat(customerSynchronizer.lastName).isEqualTo("Lovelace");
    }

    @Test
    void zitadelAdminRoleMapsToSpringAdminAuthority() {
        OidcUser oidcUser = oidcUser(Map.of(
                "sub", "subject-1",
                "email", "admin@example.com",
                "email_verified", true,
                "urn:zitadel:iam:org:project:roles", Map.of("admin", Map.of())
        ));
        customerSynchronizer.customer.setRole("USER");
        delegateUser = oidcUser;

        OidcUser result = userService.loadUser(null);

        assertThat(authorityNames(result)).contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void unverifiedZitadelEmailIsRejectedBeforeCustomerSync() {
        delegateUser = oidcUser(Map.of(
                "sub", "subject-1",
                "email", "customer@example.com",
                "email_verified", false
        ));

        assertThatThrownBy(() -> userService.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("email must be verified");
        assertThat(customerSynchronizer.calls).isZero();
    }

    @Test
    void existingLocalAdminRoleAlsoMapsToSpringAdminAuthority() {
        OidcUser oidcUser = oidcUser(Map.of(
                "sub", "subject-1",
                "email", "admin@example.com",
                "email_verified", true
        ));
        customerSynchronizer.customer.setRole("ADMIN");
        delegateUser = oidcUser;

        OidcUser result = userService.loadUser(null);

        assertThat(authorityNames(result)).contains("ROLE_ADMIN");
        assertThat(customerSynchronizer.zitadelSubject).isEqualTo("subject-1");
        assertThat(customerSynchronizer.email).isEqualTo("admin@example.com");
    }

    private OidcUser oidcUser(Map<String, Object> claims) {
        OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                claims
        );
        return new DefaultOidcUser(Set.of(), idToken, new OidcUserInfo(claims), "email");
    }

    private Set<String> authorityNames(OidcUser user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static class RecordingCustomerSynchronizer implements ZitadelCustomerSynchronizer {
        private final Customer customer = new Customer();
        private int calls;
        private String zitadelSubject;
        private String email;
        private String firstName;
        private String lastName;

        @Override
        public Customer syncZitadelCustomer(String zitadelSubject, String email, String firstName, String lastName) {
            calls++;
            this.zitadelSubject = zitadelSubject;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            return customer;
        }
    }
}
