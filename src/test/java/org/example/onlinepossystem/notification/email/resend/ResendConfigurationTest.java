package org.example.onlinepossystem.notification.email.resend;

import com.resend.services.emails.Emails;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.util.Map;
import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResendConfigurationTest {
    @ParameterizedTest
    @MethodSource("keyConfigurations")
    void bindsEffectiveKeyFromEnvironmentNames(Map<String, Object> variables, String expected) throws IOException {
        ResourcePropertySource applicationProperties = new ResourcePropertySource(
                new FileSystemResource("src/main/resources/application.properties"));
        new ApplicationContextRunner()
                .withUserConfiguration(ResendConfiguration.class)
                .withInitializer(context -> {
                    context.getEnvironment().getPropertySources().addLast(applicationProperties);
                    context.getEnvironment().getPropertySources().addFirst(
                            new SystemEnvironmentPropertySource("testEnvironment", variables));
                })
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(Emails.class);
                    assertThat(context.getBean(ResendProperties.class).getApiKey()).isEqualTo(expected);
                });
    }

    static Stream<Arguments> keyConfigurations() {
        return Stream.of(
                Arguments.of(Map.of("MAIL_API", "re_legacy", "RESEND_API_KEY", ""), "re_legacy"),
                Arguments.of(Map.of("MAIL_API", "re_legacy", "RESEND_API_KEY", "re_primary"), "re_primary"),
                Arguments.of(Map.of("MAIL_API", "", "RESEND_API_KEY", "re_primary"), "re_primary"),
                Arguments.of(Map.of("MAIL_API", "", "RESEND_API_KEY", "  re_primary  "), "re_primary"),
                Arguments.of(Map.of("RESEND_API_KEY", "re_primary"), "re_primary"),
                Arguments.of(Map.of("MAIL_API", "", "RESEND_API_KEY", ""), "")
        );
    }
}
