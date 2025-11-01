package com.simple_online_store_backend.util;

import com.simple_online_store_backend.config.JWTFilter;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.PeopleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartupSeeder implements CommandLineRunner {

    private final PeopleRepository persons;
    private final PasswordEncoder passwordEncoder;
    private final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    // --- ADMIN props ---
    @Value("${app.admin.enabled:true}") private boolean adminEnabled;
    @Value("${app.admin.username:admin}") private String adminUsername;
    @Value("${app.admin.email:admin@example.com}") private String adminEmail;
    @Value("${app.admin.password:ChangeMe_123!}") private String adminPassword;
    @Value("${app.admin.role:ROLE_ADMIN}") private String adminRole;

    // --- USER props ---
    @Value("${app.user.enabled:true}") private boolean userEnabled;
    @Value("${app.user.username:user}") private String userUsername;
    @Value("${app.user.email:user@example.com}") private String userEmail;
    @Value("${app.user.password:user123!}") private String userPassword;
    @Value("${app.user.role:ROLE_USER}") private String userRole;

    // --- reset flags ---
    @Value("${app.reset.admin-password:false}") private boolean resetAdminPassword;
    @Value("${app.reset.user-password:false}") private boolean resetUserPassword;

    public StartupSeeder(PeopleRepository persons, PasswordEncoder passwordEncoder) {
        this.persons = persons;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public void run(String... args) {
        if (adminEnabled) ensureAccount(adminEmail, adminUsername, adminRole, adminPassword, resetAdminPassword);
        else log.info("Admin seeding disabled.");

        if (userEnabled) ensureAccount(userEmail, userUsername, userRole, userPassword, resetUserPassword);
        else log.info("User seeding disabled.");
    }

    private void ensureAccount(String email, String username, String role, String rawPassword, boolean resetPasswordFlag) {
        persons.findFirstByEmail(email).ifPresentOrElse(existing -> {
            boolean changed = false;

            // выровнять username/role при необходимости
            if (!username.equals(existing.getUserName())) {
                existing.setUserName(username);
                changed = true;
            }
            if (!role.equals(existing.getRole())) {
                existing.setRole(role);
                changed = true;
            }
            if (resetPasswordFlag) {
                existing.setPassword(passwordEncoder.encode(rawPassword));
                changed = true;
                log.warn("Password reset for '{}'", email);
            }
            if (changed) persons.save(existing);
            log.info("Account '{}' exists with role {} — seeding skipped.", email, existing.getRole());
        }, () -> {
            Person p = new Person();
            p.setUserName(username);
            p.setEmail(email);
            p.setRole(role);
            p.setPassword(passwordEncoder.encode(rawPassword)); // важно: хэшируем

            persons.save(p);
            log.warn("Default account '{}' with role {} created.", email, role);
        });
    }
}

