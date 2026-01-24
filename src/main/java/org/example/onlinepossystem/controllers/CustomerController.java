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
            @RequestParam String area,             // ✅ changed from city
            @RequestParam String postalCode,
            @RequestParam String phone,
            @RequestParam(required = false) String phone2,
            @RequestParam String preferred_store,
            @RequestParam(required = false) String complex_name
    ) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPassword(password);
        customer.setHouseNumber(house_number);
        customer.setStreet(street);
        customer.setArea(area);                   // ✅ area
        customer.setPostalCode(postalCode);
        customer.setPhone(phone);                 // phone1
        customer.setPhone2(phone2);               // optional
        customer.setPreferredStore(preferred_store);
        customer.setComplexName(complex_name);   // optional
        customer.setLastOrderedAt(java.time.LocalDateTime.now());

        customerRepository.save(customer);

        return "redirect:/";  // go to index after registration
    }
}
