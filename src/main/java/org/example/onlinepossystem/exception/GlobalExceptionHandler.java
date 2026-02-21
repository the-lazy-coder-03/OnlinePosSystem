package org.example.onlinepossystem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;

import jakarta.validation.ConstraintViolationException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoSuchElementException(NoSuchElementException ex, Model model) {
        logger.error("Resource not found: {}", ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("message", "The requested resource was not found: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationExceptions(Exception ex, Model model) {
        logger.error("Validation error: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("message", "Invalid request: " + ex.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        logger.error("Bad request: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex, Model model) {
        logger.error("Data integrity violation", ex);
        model.addAttribute("status", 409);
        model.addAttribute("message", "Conflict: " + ex.getMostSpecificCause().getMessage());
        return "error";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException ex, Model model, HttpServletResponse response) {
        logger.error("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());
        response.setStatus(ex.getStatusCode().value());
        model.addAttribute("status", ex.getStatusCode().value());
        model.addAttribute("message", ex.getReason());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        logger.error("Unhandled exception occurred", ex);
        model.addAttribute("status", 500);
        model.addAttribute("message", "An unexpected error occurred: " + ex.getMessage());
        return "error";
    }
}
