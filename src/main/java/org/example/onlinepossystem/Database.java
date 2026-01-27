package org.example.onlinepossystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.onlinepossystem.repository.StaffRepository;
import org.example.onlinepossystem.service.StaffService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Component
public class Database implements CommandLineRunner {

    private final DataSource dataSource;
    private final StaffService staffService;
    private final StaffRepository staffRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Database(DataSource dataSource, StaffService staffService, StaffRepository staffRepository) {
        this.dataSource = dataSource;
        this.staffService = staffService;
        this.staffRepository = staffRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Testing database connection...");

        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Connected to database successfully!");

                syncStaffFromFile();
            } else {
                System.out.println("❌ Failed to connect to database");
            }
        } catch (SQLException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
        }
    }

    private void syncStaffFromFile() {
        File configFile = new File("staff-config.json");
        if (!configFile.exists()) {
            System.out.println("ℹ️ No staff-config.json found in root directory. Skipping staff sync.");
            return;
        }

        try {
            System.out.println("Reading staff configuration from staff-config.json...");
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
                    staffService.updateOrCreateStaff(name, branch, pin, branchCode);
                    System.out.println("Synced staff for branch: " + branch);
                }
            }
            System.out.println("✅ Staff sync complete!");
        } catch (Exception e) {
            System.err.println("❌ Failed to sync staff from file: " + e.getMessage());
        }
    }
}
