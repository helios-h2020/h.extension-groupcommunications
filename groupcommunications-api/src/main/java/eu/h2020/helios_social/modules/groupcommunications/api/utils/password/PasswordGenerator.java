package eu.h2020.helios_social.modules.groupcommunications.api.utils.password;

import java.security.SecureRandom;

public class PasswordGenerator {

    private PasswordPreferences passwordPreferences;

    public PasswordGenerator() {
        passwordPreferences = new PasswordPreferences.Builder()
                .includeUpperCaseLetters()
                .includeNumbers()
                .includeSymbols()
                .build();
    }

    public PasswordGenerator(PasswordPreferences passwordPreferences) {
        this.passwordPreferences = passwordPreferences;
    }

    public String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        if (length < 6) throw new IllegalArgumentException("Given password length < 6 not allowed");

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int rndCharAt = random.nextInt(passwordPreferences.getPasswordPrefChars().length());
            char rndChar = passwordPreferences.getPasswordPrefChars().charAt(rndCharAt);
            sb.append(rndChar);
        }

        return sb.toString();
    }

}
