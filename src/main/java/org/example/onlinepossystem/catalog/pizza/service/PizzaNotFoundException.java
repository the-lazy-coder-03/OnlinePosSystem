package org.example.onlinepossystem.catalog.pizza.service;

public class PizzaNotFoundException extends RuntimeException {
    public PizzaNotFoundException(String message) {
        super(message);
    }
}
