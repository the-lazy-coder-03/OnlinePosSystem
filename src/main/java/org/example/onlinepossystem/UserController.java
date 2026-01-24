package org.example.onlinepossystem.controller;

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
}
