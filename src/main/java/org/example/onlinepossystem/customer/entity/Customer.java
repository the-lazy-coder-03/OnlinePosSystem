package org.example.onlinepossystem.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone1", unique = true)
    private String phone1;

    @Column(name = "phone2")
    private String phone2;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "house_number")
    private String houseNumber;

    @Column(name = "street")
    private String street;

    @Column(name = "area")
    private String area;

    @Column(name = "complex_name")
    private String complexName;

    @Column(name = "last_ordered_at")
    private LocalDateTime lastOrderedAt;

    @Column(name = "preferred_store")
    private String preferredStore;

    @Column(name = "city")
    private String city;

    @Column(name = "password")
    private String password;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "formatted_address", columnDefinition = "TEXT")
    private String formattedAddress;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(name = "province")
    private String province;

    @Column(name = "country")
    private String country;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "role")
    private String role = "USER";

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.SMALLINT)
    @Column(name = "access_level", nullable = false, columnDefinition = "smallint")
    private Integer accessLevel = 0;

    @Column(name = "environment_admin", nullable = false)
    private boolean environmentAdmin;

    // Default constructor
    public Customer() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getName() { return firstName + " " + lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getPhone1() { return phone1; }
    public void setPhone1(String phone1) { this.phone1 = phone1; }

    public String getPhone2() { return phone2; }
    public void setPhone2(String phone2) { this.phone2 = phone2; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getGooglePlaceId() { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }

    public String getFormattedAddress() { return formattedAddress; }
    public void setFormattedAddress(String formattedAddress) { this.formattedAddress = formattedAddress; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPreferredStore() { return preferredStore; }
    public void setPreferredStore(String preferredStore) { this.preferredStore = preferredStore; }

    public LocalDateTime getLastOrderedAt() { return lastOrderedAt; }
    public void setLastOrderedAt(LocalDateTime lastOrderedAt) { this.lastOrderedAt = lastOrderedAt; }

    public String getComplexName() { return complexName; }
    public void setComplexName(String complexName) { this.complexName = complexName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getAccessLevel() { return accessLevel; }
    public void setAccessLevel(Integer accessLevel) { this.accessLevel = accessLevel; }

    public boolean isEnvironmentAdmin() { return environmentAdmin; }
    public void setEnvironmentAdmin(boolean environmentAdmin) { this.environmentAdmin = environmentAdmin; }
}
