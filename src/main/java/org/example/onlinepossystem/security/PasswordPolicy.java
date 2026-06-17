package org.example.onlinepossystem.security;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final String MESSAGE = "Password must be at least 8 characters and include uppercase, lowercase, number, and special character.";

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern NUMBER = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && password.length() >= 8
                && UPPERCASE.matcher(password).find()
                && LOWERCASE.matcher(password).find()
                && NUMBER.matcher(password).find()
                && SPECIAL.matcher(password).find();
    }
}
