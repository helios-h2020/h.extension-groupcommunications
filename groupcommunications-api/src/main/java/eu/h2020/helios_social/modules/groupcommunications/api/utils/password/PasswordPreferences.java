package eu.h2020.helios_social.modules.groupcommunications.api.utils.password;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PasswordPreferences {

    private String PASSWORD_BASE;

    private PasswordPreferences(String password_base) {
        this.PASSWORD_BASE = shuffleString(password_base);
    }

    public String getPasswordPrefChars() {
        return PASSWORD_BASE;
    }

    public String reShufflePasswordPrefChars() {
        return shuffleString(PASSWORD_BASE);
    }

    private static String shuffleString(String string) {
        List<String> letters = Arrays.asList(string.split(""));
        Collections.shuffle(letters);
        return letters.stream().collect(Collectors.joining());
    }

    public static class Builder {

        private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
        private static final String UPPERCASE_LETTERS = LOWERCASE_LETTERS.toUpperCase();
        private static final String NUMBERS = "0123456789";
        private static final String SYMBOLS = "!@#$%&*()_+-=[]?";

        private String ALLOWED_PASSWORD_CHARACTERS;

        public Builder() {
            ALLOWED_PASSWORD_CHARACTERS += LOWERCASE_LETTERS;
        }

        public Builder includeUpperCaseLetters() {
            ALLOWED_PASSWORD_CHARACTERS += UPPERCASE_LETTERS;
            return this;
        }

        public Builder includeNumbers() {
            ALLOWED_PASSWORD_CHARACTERS += NUMBERS;
            return this;
        }

        public Builder includeSymbols() {
            ALLOWED_PASSWORD_CHARACTERS += SYMBOLS;
            return this;
        }

        public PasswordPreferences build() {
            return new PasswordPreferences(ALLOWED_PASSWORD_CHARACTERS);
        }
    }
}
