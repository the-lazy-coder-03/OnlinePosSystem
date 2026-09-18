package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.customer.api.CustomerAccount;
import org.example.onlinepossystem.customer.api.CustomerOrderRecorder;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.security.api.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class CustomerService implements CustomerAccountReader, CustomerOrderRecorder, ZitadelCustomerSynchronizer {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           PasswordPolicy passwordPolicy) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

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

        if (!passwordPolicy.isValid(password)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.PASSWORD, passwordPolicy.validationMessage());
        }
        if (emailExists(email)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.EMAIL, "Email is already registered.");
        }
        if (phoneExists(phone)) {
            throw new CustomerRegistrationException(CustomerRegistrationException.Reason.PHONE, "Phone number is already registered.");
        }

        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode(password)); // encode password
        customer.setPhone1(phone);        // main phone
        customer.setPhone2(phone2);       // optional second phone
        customer.setHouseNumber(houseNumber);
        customer.setStreet(street);
        customer.setArea(area);
        customer.setComplexName(complex);
        customer.setPreferredStore(preferredStore);
        customer.setPostalCode(postalCode);
        customer.setLastOrderedAt(LocalDateTime.now());
        customer.setRole("USER");

        return customerRepository.save(customer); // insert into DB
    }

    @Override
    public Optional<CustomerAccount> findByEmail(String email) {
        return customerRepository.findByEmail(email).map(this::toAccount);
    }

    @Override
    public Optional<CustomerAccount> findById(Long id) {
        return customerRepository.findById(id).map(this::toAccount);
    }

    @Override
    public void recordOrderPlaced(Long customerId, LocalDateTime orderedAt) {
        customerRepository.findById(customerId).ifPresent(customer -> {
            customer.setLastOrderedAt(orderedAt);
            customerRepository.save(customer);
        });
    }

    public boolean emailExists(String email) {
        return customerRepository.findByEmail(email).isPresent();
    }

    public boolean phoneExists(String phone) {
        return customerRepository.findByPhone1(phone).isPresent();
    }

    @Override
    public Customer syncZitadelCustomer(String zitadelSubject,
                                        String email,
                                        String firstName,
                                        String lastName) {
        String normalizedSubject = requireText(zitadelSubject, "Zitadel subject");
        String normalizedEmail = normalizeEmail(email);

        Optional<Customer> customerBySubject = customerRepository.findByZitadelSubject(normalizedSubject);
        Optional<Customer> customerByEmail = customerRepository.findByEmail(normalizedEmail);

        Customer customer;
        if (customerBySubject.isPresent()) {
            customer = customerBySubject.get();
            if (customerByEmail.isPresent() && !customerByEmail.get().getId().equals(customer.getId())) {
                throw new IllegalStateException("Zitadel email is already linked to another customer.");
            }
        } else {
            customer = customerByEmail.orElseGet(Customer::new);
            customer.setZitadelSubject(normalizedSubject);
        }

        customer.setEmail(normalizedEmail);
        updateName(customer, firstName, lastName);
        if (customer.getRole() == null || customer.getRole().isBlank()) {
            customer.setRole("USER");
        }
        if (customer.getLastOrderedAt() == null) {
            customer.setLastOrderedAt(LocalDateTime.now());
        }

        return customerRepository.save(customer);
    }

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
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new java.util.NoSuchElementException("Customer not found for email: " + email));

        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setHouseNumber(houseNumber);
        customer.setStreet(street);
        customer.setArea(area);
        customer.setPostalCode(postalCode);
        customer.setPhone1(phone1);
        customer.setPhone2(phone2);
        customer.setPreferredStore(preferredStore);
        customer.setComplexName(complexName);

        if (newPassword != null && !newPassword.isEmpty()) {
            if (!passwordPolicy.isValid(newPassword)) {
                throw new IllegalArgumentException(passwordPolicy.validationMessage());
            }
            customer.setPassword(passwordEncoder.encode(newPassword));
        }

        return customerRepository.save(customer);
    }

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return requireText(email, "Email").toLowerCase(Locale.ROOT);
    }

    private void updateName(Customer customer, String firstName, String lastName) {
        if (firstName != null && !firstName.isBlank()) {
            customer.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            customer.setLastName(lastName.trim());
        }
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
                customer.getFirstName(),
                customer.getLastName(),
                customer.getRole()
        );
    }
}
