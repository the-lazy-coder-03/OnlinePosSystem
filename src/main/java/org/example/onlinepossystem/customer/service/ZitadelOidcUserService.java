package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ZitadelOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private static final String DEFAULT_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private static final String ADMIN_ROLE = "admin";

    private final ZitadelCustomerSynchronizer customerSynchronizer;
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final String rolesClaim;

    @Autowired
    public ZitadelOidcUserService(
            ZitadelCustomerSynchronizer customerSynchronizer,
            @Value("${app.zitadel.roles-claim:" + DEFAULT_ROLES_CLAIM + "}") String rolesClaim
    ) {
        this(customerSynchronizer, rolesClaim, new OidcUserService());
    }

    ZitadelOidcUserService(
            ZitadelCustomerSynchronizer customerSynchronizer,
            String rolesClaim,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate
    ) {
        this.customerSynchronizer = customerSynchronizer;
        this.rolesClaim = rolesClaim;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser user = delegate.loadUser(userRequest);
        Map<String, Object> claims = user.getClaims();
        String subject = user.getSubject();
        String email = claimAsString(claims, "email");

        if (!isEmailVerified(claims)) {
            throw invalidUser("Zitadel email must be verified before signing in.");
        }
        if (email == null || email.isBlank()) {
            throw invalidUser("Zitadel did not return an email address.");
        }

        Customer customer = customerSynchronizer.syncZitadelCustomer(
                subject,
                email,
                claimAsString(claims, "given_name"),
                claimAsString(claims, "family_name")
        );

        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (hasAdminRole(claims) || isAdminCustomer(customer)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), "email");
    }

    private boolean isEmailVerified(Map<String, Object> claims) {
        Object value = claims.get("email_verified");
        if (value instanceof Boolean verified) {
            return verified;
        }
        return value instanceof String text && Boolean.parseBoolean(text);
    }

    private boolean hasAdminRole(Map<String, Object> claims) {
        Object configuredRoles = claims.get(rolesClaim);
        Object defaultRoles = claims.get(DEFAULT_ROLES_CLAIM);
        Object directAdminRole = claims.get("urn:zitadel:iam:org:project:role:admin");
        return containsRole(configuredRoles, ADMIN_ROLE)
                || containsRole(defaultRoles, ADMIN_ROLE)
                || Boolean.TRUE.equals(directAdminRole)
                || containsRole(directAdminRole, ADMIN_ROLE);
    }

    private boolean containsRole(Object claimValue, String expectedRole) {
        if (claimValue == null) {
            return false;
        }
        if (claimValue instanceof Map<?, ?> roles) {
            return roles.keySet().stream().anyMatch(role -> roleMatches(role, expectedRole));
        }
        if (claimValue instanceof Collection<?> roles) {
            return roles.stream().anyMatch(role -> containsRole(role, expectedRole));
        }
        return roleMatches(claimValue, expectedRole);
    }

    private boolean roleMatches(Object role, String expectedRole) {
        return role != null && expectedRole.equals(role.toString().trim().toLowerCase(Locale.ROOT));
    }

    private boolean isAdminCustomer(Customer customer) {
        return customer.getRole() != null
                && "ADMIN".equals(customer.getRole().replace("ROLE_", "").trim().toUpperCase(Locale.ROOT));
    }

    private String claimAsString(Map<String, Object> claims, String claim) {
        Object value = claims.get(claim);
        return value == null ? null : value.toString();
    }

    private OAuth2AuthenticationException invalidUser(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_zitadel_user"), message);
    }
}
