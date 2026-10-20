package org.example.onlinepossystem.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPasswordPolicyTest {
    private final DefaultPasswordPolicy policy = new DefaultPasswordPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"Abcdefg1", "ValidPass1", "ValidPass1!"})
    void acceptsPasswordsWithOptionalSpecialCharacters(String password) {
        assertThat(policy.isValid(password)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Abcdef1", "abcdefg1", "ABCDEFG1", "Abcdefgh", "12345678", "!!!!!!!!"})
    void rejectsPasswordsMissingRemainingRequirements(String password) {
        assertThat(policy.isValid(password)).isFalse();
    }
}
