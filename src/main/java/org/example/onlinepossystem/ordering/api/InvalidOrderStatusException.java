package org.example.onlinepossystem.ordering.api;

public class InvalidOrderStatusException extends IllegalArgumentException {
    public InvalidOrderStatusException() {
        super("Invalid status. Must be Pending, Preparing, Completed, or Rejected.");
    }
}
