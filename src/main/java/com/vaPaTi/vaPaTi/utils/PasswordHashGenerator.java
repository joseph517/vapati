package com.vaPaTi.vaPaTi.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Standalone CLI utility to generate a BCrypt password hash compatible with
 * {@link com.vaPaTi.vaPaTi.security.SecurityConfig}'s PasswordEncoder bean.
 * Not a Spring bean and not exposed via any endpoint — intended to be run
 * locally to prepare the password value used in scripts/create-admin-user.sql.
 *
 * Usage:
 *   java -cp target/classes:$(./mvnw -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) \
 *        com.vaPaTi.vaPaTi.utils.PasswordHashGenerator "miPasswordSegura"
 */
public final class PasswordHashGenerator {

    private PasswordHashGenerator() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ... PasswordHashGenerator <plainTextPassword>");
            System.exit(1);
        }
        String hash = new BCryptPasswordEncoder().encode(args[0]);
        System.out.println(hash);
    }
}
