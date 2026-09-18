package org.example.onlinepossystem.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnProperty(name = "app.zitadel.enabled", havingValue = "true")
public class ZitadelOAuthClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.zitadel.issuer-uri}") String issuerUri,
            @Value("${app.zitadel.client-id}") String clientId,
            @Value("${app.zitadel.client-secret}") String clientSecret
    ) {
        requireConfigured(issuerUri, "ZITADEL_ISSUER_URI");
        requireConfigured(clientId, "ZITADEL_CLIENT_ID");
        requireConfigured(clientSecret, "ZITADEL_CLIENT_SECRET");

        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(issuerUri)
                .registrationId("zitadel")
                .clientName("Zitadel")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email", "urn:zitadel:iam:org:project:roles")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    private void requireConfigured(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must be set when ZITADEL_ENABLED=true.");
        }
    }
}
