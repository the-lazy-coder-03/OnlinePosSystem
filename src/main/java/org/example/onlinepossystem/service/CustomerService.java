package org.example.onlinepossystem.service;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer registerCustomer(String name,
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

        Customer customer = new Customer();
        customer.setName(name);
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

        return customerRepository.save(customer); // insert into DB
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}
