package org.example.onlinepossystem.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFoundException(NoResourceFoundException ex, Model model) {
        logger.info("Resource not found: {}", ex.getResourcePath());
        model.addAttribute("status", 404);
        model.addAttribute("message", "The requested resource was not found.");
        return "error";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoSuchElementException(NoSuchElementException ex, Model model) {
        logger.error("Resource not found: {}", ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("message", "The requested resource was not found.");
        return "error";
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationExceptions(Exception ex, Model model) {
        logger.error("Validation error: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("message", "Please check your input and try again.");
        return "error";
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMissingRequestParameter(MissingServletRequestParameterException ex, Model model) {
        logger.error("Missing request parameter: {}", ex.getParameterName());
        model.addAttribute("status", 400);
        model.addAttribute("message", "Please check your input and try again.");
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        logger.error("Bad request: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("message", "Please check your input and try again.");
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex, Model model, HttpServletResponse response) {
        if (isDatabaseAuthorizationFailure(ex)) {
            response.setStatus(403);
            model.addAttribute("status", 403);
            model.addAttribute("message", "You do not have permission to perform this action.");
            return "error";
        }
        logger.error("Data integrity violation", ex);
        response.setStatus(409);
        model.addAttribute("status", 409);
        model.addAttribute("message", "That request could not be completed because it conflicts with existing data.");
        return "error";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException ex, Model model, HttpServletResponse response) {
        logger.error("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());
        response.setStatus(ex.getStatusCode().value());
        model.addAttribute("status", ex.getStatusCode().value());
        model.addAttribute("message", ex.getStatusCode().is4xxClientError()
                ? "Please check your input and try again."
                : "An unexpected error occurred.");
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        logger.warn("Access denied: {}", ex.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("message", "You do not have permission to perform this action.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model, HttpServletResponse response) {
        if (isDatabaseAuthorizationFailure(ex)) {
            response.setStatus(403);
            model.addAttribute("status", 403);
            model.addAttribute("message", "You do not have permission to perform this action.");
            return "error";
        }
        logger.error("Unhandled exception occurred", ex);
        model.addAttribute("status", 500);
        model.addAttribute("message", "An unexpected error occurred. Please try again later.");
        return "error";
    }
    private boolean isDatabaseAuthorizationFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && "42501".equals(sql.getSQLState())) return true;
        }
        return false;
    }

}
