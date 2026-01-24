package org.example.onlinepossystem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class Database implements CommandLineRunner {

    private final DataSource dataSource;

    public Database(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Testing database connection...");

        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connected to Supabase successfully!");
                System.out.println("DB Product Name: " + conn.getMetaData().getDatabaseProductName());
                System.out.println("DB Version: " + conn.getMetaData().getDatabaseProductVersion());
            } else {
                System.out.println("❌ Failed to connect to Supabase");
            }
        } catch (SQLException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
