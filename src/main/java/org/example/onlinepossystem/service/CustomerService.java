package org.example.onlinepossystem.service;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.customerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final customerRepository customerRepository;

    public CustomerService(customerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer registerCustomer(String name,
                                     String email,
                                     String phone,
                                     String phone2,
                                     String houseNumber,
                                     String street,
                                     String city,
                                     String complex,
                                     String preferredStore,
                                     String postalCode) {

        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setPhone2(phone2);
        customer.setHouseNumber(houseNumber);
        customer.setStreet(street);
        customer.setCity(city);
        customer.setComplexName(complex);
        customer.setPreferredStore(preferredStore);
        customer.setPostalCode(postalCode);
        customer.setLastOrderedAt(LocalDateTime.now());

        return customerRepository.save(customer); // ← this inserts into DB
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}
