package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.customer.api.AddressSelection;
import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.CustomerOrderRecorder;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.security.api.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.onlinepossystem.customer.persistence.AccountBootstrapStore;
import org.example.onlinepossystem.customer.api.AccountCredentialsChanged;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CustomerService implements CustomerAccountReader, CustomerOrderRecorder {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AccountBootstrapStore accounts;
    private final ApplicationEventPublisher events;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           PasswordPolicy passwordPolicy, AccountBootstrapStore accounts,
                           ApplicationEventPublisher events) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public Customer registerCustomer(String firstName,
                                     String lastName,
                                     String email,
                                     String password,
                                     String phone,
                                     String phone2,
                                     String houseNumber,
                                     String street,
                                     String area,
                                     String complex,
                                     String preferredStore,
                                     String postalCode) {
        return registerCustomer(firstName, lastName, email, password, phone, phone2, houseNumber, street, area,
                complex, preferredStore, postalCode, AddressSelection.empty());
    }

    @Transactional
    public Customer registerCustomer(String firstName,
                                     String lastName,
                                     String email,
                                     String password,
                                     String phone,
                                     String phone2,
                                     String houseNumber,
                                     String street,
                                     String area,
                                     String complex,
                                     String preferredStore,
                                     String postalCode,
                                     AddressSelection address) {

        if (!passwordPolicy.isValid(password)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.PASSWORD, passwordPolicy.validationMessage());
        }
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (emailExists(normalizedEmail)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.EMAIL, "Email is already registered.");
        }
        if (phoneExists(phone)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.PHONE, "Phone number is already registered.");
        }
        if (address != null && address.hasGoogleSelection() && !address.isVerifiedGoogleAddress()) {
            throw new IllegalArgumentException("A verified Google address selection is required.");
        }

        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(normalizedEmail);
        customer.setPassword(passwordEncoder.encode(password)); // encode password
        customer.setPhone1(phone);        // main phone
        customer.setPhone2(phone2);       // optional second phone
        customer.setHouseNumber(houseNumber);
        customer.setStreet(street);
        customer.setArea(area);
        customer.setComplexName(complex);
        customer.setPreferredStore(preferredStore);
        customer.setPostalCode(postalCode);
        if (address != null && address.hasGoogleSelection()) {
            customer.setCity(address.city());
        }
        applyAddressMetadata(customer, address);
        customer.setLastOrderedAt(LocalDateTime.now());
        customer.setRole("USER");
        customer.setAccessLevel(0);

        return accounts.register(customer);
    }

    @Override
    public Optional<CustomerAccount> findByEmail(String email) {
        return customerRepository.findByEmailIgnoreCase(email).map(this::toAccount);
    }

    @Override
    public Optional<CustomerAccount> findById(Long id) {
        return customerRepository.findById(id).map(this::toAccount);
    }

    @Override
    @Transactional
    public void recordOrderPlaced(Long customerId, LocalDateTime orderedAt) {
        customerRepository.findById(customerId).ifPresent(customer -> {
            customer.setLastOrderedAt(orderedAt);
            customerRepository.save(customer);
        });
    }

    public boolean emailExists(String email) {
        return accounts.emailExists(email);
    }

    public boolean phoneExists(String phone) {
        return accounts.phoneExists(phone);
    }

    @Transactional
    public Customer updateProfile(String email,
                                  String firstName,
                                  String lastName,
                                  String houseNumber,
                                  String street,
                                  String area,
                                  String postalCode,
                                  String phone1,
                                  String phone2,
                                  String preferredStore,
                                  String complexName,
                                  String newPassword) {
        return updateProfile(email, firstName, lastName, houseNumber, street, area, postalCode, phone1, phone2,
                preferredStore, complexName, newPassword, AddressSelection.empty());
    }

    @Transactional
    public Customer updateProfile(String email,
                                  String firstName,
                                  String lastName,
                                  String houseNumber,
                                  String street,
                                  String area,
                                  String postalCode,
                                  String phone1,
                                  String phone2,
                                  String preferredStore,
                                  String complexName,
                                  String newPassword,
                                  AddressSelection address) {
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new java.util.NoSuchElementException("Customer not found for email: " + email));

        boolean addressSubmitted = houseNumber != null
                || street != null
                || area != null
                || postalCode != null
                || complexName != null
                || (address != null && (address.city() != null || address.hasGoogleSelection()));
        boolean addressChanged = addressSubmitted && (!same(customer.getHouseNumber(), houseNumber)
                || !same(customer.getStreet(), street)
                || !same(customer.getArea(), area)
                || (address != null && address.city() != null && !same(customer.getCity(), address.city()))
                || !same(customer.getPostalCode(), postalCode));
        if ((address == null || !address.hasGoogleSelection()) && addressChanged) {
            throw new IllegalArgumentException("A verified Google address selection is required.");
        }
        if (address != null && address.hasGoogleSelection() && !address.isVerifiedGoogleAddress()) {
            throw new IllegalArgumentException("A verified Google address selection is required.");
        }

        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        if (addressSubmitted) {
            customer.setHouseNumber(houseNumber);
            customer.setStreet(street);
            customer.setArea(area);
            customer.setPostalCode(postalCode);
            customer.setComplexName(complexName);
        }
        customer.setPhone1(phone1);
        customer.setPhone2(phone2);
        customer.setPreferredStore(preferredStore);
        if (address != null && address.hasGoogleSelection()) {
            customer.setCity(address.city());
        }
        applyAddressMetadata(customer, address);

        boolean passwordChanged = newPassword != null && !newPassword.isEmpty();
        if (passwordChanged) {
            if (!passwordPolicy.isValid(newPassword)) {
                throw new IllegalArgumentException(passwordPolicy.validationMessage());
            }
            customer.setPassword(passwordEncoder.encode(newPassword));
        }

        Customer saved = customerRepository.save(customer);
        if (passwordChanged) events.publishEvent(new AccountCredentialsChanged(saved.getEmail()));
        return saved;
    }

    private CustomerAccount toAccount(Customer customer) {
        return new CustomerAccount(
                customer.getId(),
                customer.getPhone1(),
                customer.getPhone2(),
                customer.getEmail(),
                customer.getHouseNumber(),
                customer.getStreet(),
                customer.getArea(),
                customer.getComplexName(),
                customer.getLastOrderedAt(),
                customer.getPreferredStore(),
                customer.getCity(),
                customer.getPostalCode(),
                customer.getGooglePlaceId(),
                customer.getFormattedAddress(),
                customer.getLatitude(),
                customer.getLongitude(),
                customer.getProvince(),
                customer.getCountry(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getRole(),
                customer.getAccessLevel() == null ? 0 : customer.getAccessLevel()
        );
    }

    private void applyAddressMetadata(Customer customer, AddressSelection address) {
        if (address == null || !address.hasGoogleSelection()) {
            return;
        }
        customer.setGooglePlaceId(address.googlePlaceId());
        customer.setFormattedAddress(address.formattedAddress());
        customer.setLatitude(address.latitude());
        customer.setLongitude(address.longitude());
        customer.setProvince(address.province());
        customer.setCountry(address.country());
    }

    private boolean same(String stored, String submitted) {
        return normalize(stored).equals(normalize(submitted));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
