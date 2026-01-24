package org.example.onlinepossystem;


import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

                // Create table if it doesn't exist
                String createTable = "CREATE TABLE IF NOT EXISTS baddies (" +
                        "id BIGSERIAL PRIMARY KEY, " +
                        "name TEXT, " +
                        "surname TEXT" +
                        ")";
                try (PreparedStatement stmt = conn.prepareStatement(createTable)) {
                    stmt.execute();
                    System.out.println("Table 'baddies' ready!");
                }

                // Insert test row
                String insertSql = "INSERT INTO baddies (name, surname) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, "TestName");
                    insertStmt.setString(2, "TestSurname");
                    insertStmt.executeUpdate();
                    System.out.println("Inserted test row!");
                }

                // Read back test row
                String selectSql = "SELECT id, name, surname FROM baddies ORDER BY id DESC LIMIT 1";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    ResultSet rs = selectStmt.executeQuery();
                    while (rs.next()) {
                        System.out.println("Last Row -> ID: " + rs.getLong("id") +
                                ", Name: " + rs.getString("name") +
                                ", Surname: " + rs.getString("surname"));
                    }
                }

            } else {
                System.out.println("❌ Failed to connect to Supabase");
            }
        } catch (SQLException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
