package org.example.onlinepossystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @Column(name = "house_number")
    private String houseNumber;

    private String street;
    private String city;

    private String phone;
    private String phone2;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "preferred_store")
    private String preferredStore;

    @Column(name = "last_ordered_at")
    private LocalDateTime lastOrderedAt;

    @Column(name = "complex_name")  // maps this field to the DB column complex_name
    private String complexName;

    private String area;

    // Default constructor
    public Customer() {}

    // Getters & Setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhone2() { return phone2; }
    public void setPhone2(String phone2) { this.phone2 = phone2; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getPreferredStore() { return preferredStore; }
    public void setPreferredStore(String preferredStore) { this.preferredStore = preferredStore; }

    public LocalDateTime getLastOrderedAt() { return lastOrderedAt; }
    public void setLastOrderedAt(LocalDateTime lastOrderedAt) { this.lastOrderedAt = lastOrderedAt; }

    public String getComplexName() { return complexName; }
    public void setComplexName(String complexName) { this.complexName = complexName; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
}
