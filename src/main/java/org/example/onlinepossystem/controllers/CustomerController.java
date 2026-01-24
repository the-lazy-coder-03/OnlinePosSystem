package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.Customer;
import org.example.onlinepossystem.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String house_number,
            @RequestParam String street,
            @RequestParam String city,
            @RequestParam String postalCode,
            @RequestParam String phone,
            @RequestParam String preferred_store
    ) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPassword(password);
        customer.setHouseNumber(house_number);
        customer.setStreet(street);
        customer.setCity(city);
        customer.setPostalCode(postalCode);
        customer.setPhone(phone);
        customer.setPreferredStore(preferred_store);

        customerRepository.save(customer); // ← writes to DB

        return "redirect:/"; // redirect to index page
    }
}
