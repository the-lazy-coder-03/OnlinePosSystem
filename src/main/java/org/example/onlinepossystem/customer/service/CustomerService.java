package org.example.onlinepossystem.customer.service;

import org.example.onlinepossystem.customer.api.CustomerAccountReader;
import org.example.onlinepossystem.customer.entity.Customer;
import org.example.onlinepossystem.customer.repository.CustomerRepository;
import org.example.onlinepossystem.security.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService implements CustomerAccountReader {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
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

        if (!PasswordPolicy.isValid(password)) {
            throw new IllegalArgumentException(PasswordPolicy.MESSAGE);
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

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return customerRepository.findByEmail(email).isPresent();
    }

    public boolean phoneExists(String phone) {
        return customerRepository.findByPhone1(phone).isPresent();
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
            if (!PasswordPolicy.isValid(newPassword)) {
                throw new IllegalArgumentException(PasswordPolicy.MESSAGE);
            }
            customer.setPassword(passwordEncoder.encode(newPassword));
        }

        return customerRepository.save(customer);
    }
}
