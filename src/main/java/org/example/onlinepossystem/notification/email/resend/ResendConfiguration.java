package org.example.onlinepossystem.notification.email.resend;

import com.resend.Resend;
import com.resend.services.emails.Emails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(ResendProperties.class)
public class ResendConfiguration {
    @Bean
    public Emails resendEmails(ResendProperties properties, @Value("${MAIL_API:}") String mailApi) {
        // Retain MAIL_API only for older installations. A blank environment
        // override must not hide an otherwise configured legacy key.
        String apiKey = StringUtils.hasText(properties.getApiKey()) ? properties.getApiKey() : mailApi;
        properties.setApiKey(apiKey == null ? "" : apiKey.trim());
        return new Resend(properties.getApiKey()).emails();
    }
}
