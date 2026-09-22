package org.example.onlinepossystem.security.rls;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.rls.enabled", havingValue = "true", matchIfMissing = true)
public class RlsConfiguration {
    @Bean
    public RlsContextInitializer rlsContextInitializer() { return new RlsContextInitializer(); }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory,
                                                   RlsContextInitializer initializer) {
        return new JpaTransactionManager(entityManagerFactory) {
            @Override
            public void afterPropertiesSet() {
                super.afterPropertiesSet();
                // JpaTransactionManager resets the dialect from the factory during initialization.
                setJpaDialect(new RlsJpaDialect(initializer));
            }
        };
    }
}
