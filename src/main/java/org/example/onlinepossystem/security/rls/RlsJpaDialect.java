package org.example.onlinepossystem.security.rls;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;

import java.sql.SQLException;

public final class RlsJpaDialect extends HibernateJpaDialect {
    private final RlsContextInitializer initializer;

    public RlsJpaDialect(RlsContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition) throws SQLException {
        Object transactionData = super.beginTransaction(entityManager, definition);
        try {
            entityManager.unwrap(Session.class).doWork(initializer::initialize);
            return transactionData;
        } catch (RuntimeException failure) {
            try {
                if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            } finally {
                super.cleanupTransaction(transactionData);
            }
            throw failure;
        }
    }
}
