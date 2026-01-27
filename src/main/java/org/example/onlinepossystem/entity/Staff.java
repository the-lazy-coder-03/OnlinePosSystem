package org.example.onlinepossystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String branch; // "Kenridge" or "Uitzicht"

    @Column(nullable = false, name = "pin_hash")
    private String pinHash; // BCrypt hashed PIN (never store plain text)

    @Column(name = "branch_code", length = 16)
    private String branchCode;

    // Default constructor
    public Staff() {}

    // Constructor with fields
    public Staff(String name, String branch, String pinHash) {
        this.name = name;
        this.branch = branch;
        this.pinHash = pinHash;
    }

    public Staff(String name, String branch, String pinHash, String branchCode) {
        this.name = name;
        this.branch = branch;
        this.pinHash = pinHash;
        this.branchCode = branchCode;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }
}
