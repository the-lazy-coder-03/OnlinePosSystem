package org.example.onlinepossystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String branch; // "Kenridge" or "Uitzicht"

    @Column(nullable = false, name = "customer_name")
    private String customerName;

    @Column(nullable = false)
    private String type; // "Pickup" or "Delivery"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String items; // JSON string or comma-separated, e.g., "[\"Pizza\",\"Soda\"]"

    @Column(nullable = false)
    private String status; // "Pending", "Accepted", "Rejected"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public Order() {
        this.createdAt = LocalDateTime.now();
        this.status = "Pending"; // Default status
    }

    // Constructor with fields
    public Order(String branch, String customerName, String type, String items) {
        this.branch = branch;
        this.customerName = customerName;
        this.type = type;
        this.items = items;
        this.status = "Pending";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
