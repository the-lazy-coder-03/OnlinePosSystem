package org.example.onlinepossystem.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.onlinepossystem.catalog.api.CatalogSeeder;
import org.example.onlinepossystem.staff.api.StaffDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Component
@org.springframework.context.annotation.Profile("postgres-test")
public class Database implements CommandLineRunner {

    private final DataSource dataSource;
    private final StaffDirectory staffDirectory;
    private final CatalogSeeder catalogSeeder;
    private final ObjectMapper objectMapper;
    private final String staffConfigPath;

    public Database(DataSource dataSource,
                    StaffDirectory staffDirectory,
                    CatalogSeeder catalogSeeder,
                    ObjectMapper objectMapper,
                    @Value("${app.staff-config-path}") String staffConfigPath) {
        this.dataSource = dataSource;
        this.staffDirectory = staffDirectory;
        this.catalogSeeder = catalogSeeder;
        this.objectMapper = objectMapper;
        this.staffConfigPath = staffConfigPath;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Testing database connection...");

        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connected to database successfully!");

                // Legacy staff credentials are no longer used for authentication.
                catalogSeeder.seedMenuData();
            } else {
                System.out.println("❌ Failed to connect to database");
            }
        } catch (SQLException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
        }
    }

    private void syncStaffFromFile() {
        File configFile = new File(staffConfigPath);
        if (!configFile.exists()) {
            System.out.println("ℹ️ No staff configuration found at " + staffConfigPath + ". Skipping staff sync.");
            return;
        }

        try {
            System.out.println("Reading staff configuration from " + staffConfigPath + "...");
            List<Map<String, String>> staffConfigs = objectMapper.readValue(
                    configFile,
                    new TypeReference<List<Map<String, String>>>() {}
            );

            for (Map<String, String> config : staffConfigs) {
                String name = config.get("name");
                String branch = config.get("branch");
                String pin = config.get("pin");
                String branchCode = config.get("branchCode");

                if (name != null && branch != null) {
                    staffDirectory.updateOrCreateStaff(name, branch, pin, branchCode);
                    System.out.println("Synced staff for branch: " + branch);
                }
            }
            System.out.println("✅ Staff sync complete!");
        } catch (Exception e) {
            System.err.println("❌ Failed to sync staff from file : " + e.getMessage());
        }
    }
}
