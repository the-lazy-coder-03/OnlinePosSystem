package org.example.onlinepossystem.controllers;

import org.example.onlinepossystem.entity.User;
import org.example.onlinepossystem.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/add")
    public User addUser(@RequestParam String name, @RequestParam String email){
        return userService.addUser(name, email);
    }

    @GetMapping("/all")
    public List<User> getUsers(){
        return userService.getAllUsers();
    }
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register"; // Thymeleaf will look for templates/register.html
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam String phone1,
                                 @RequestParam String phone2,
                                 @RequestParam String houseNumber,
                                 @RequestParam String street,
                                 @RequestParam String area,
                                 @RequestParam String complex) {
        // save user to database
        return "redirect:/success"; // redirect to a success page
    }
}
