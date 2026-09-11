package org.example.onlinepossystem.catalog.pizza.service;

public class InvalidPizzaSelectionException extends IllegalArgumentException {
    public InvalidPizzaSelectionException(String message) {
        super(message);
    }
}
