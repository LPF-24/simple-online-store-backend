package com.simple_online_store_backend.util.seeders;

import com.simple_online_store_backend.config.JWTFilter;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.PeopleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class StartupSeeder implements CommandLineRunner {

    private final PeopleRepository persons;
    private final PasswordEncoder passwordEncoder;
    private final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    // --- ADMIN props ---
    @Value("${app.admin.enabled}") private boolean adminEnabled;
    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.email}") private String adminEmail;
    @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.admin.role}") private String adminRole;

    // --- USER props ---
    @Value("${app.user.enabled}") private boolean userEnabled;
    @Value("${app.user.username}") private String userUsername;
    @Value("${app.user.email}") private String userEmail;
    @Value("${app.user.password}") private String userPassword;
    @Value("${app.user.role}") private String userRole;

    // --- BLOCKED props ---
    @Value("${app.blocked.enabled}") private boolean blockedEnabled;
    @Value("${app.blocked.username}") private String blockedUsername;
    @Value("${app.blocked.email}") private String blockedEmail;
    @Value("${app.blocked.password}") private String blockedPassword;
    @Value("${app.blocked.role}") private String blockedRole;

    // --- reset flags ---
    @Value("${app.reset.admin-password}") private boolean resetAdminPassword;
    @Value("${app.reset.user-password}") private boolean resetUserPassword;
    @Value("${app.reset.blocked-password}") private boolean resetBlockedPassword;

    public StartupSeeder(PeopleRepository persons, PasswordEncoder passwordEncoder) {
        this.persons = persons;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public void run(String... args) {
        if (adminEnabled) ensureAccount(adminEmail, adminUsername, adminRole, adminPassword, resetAdminPassword, false);
        else log.info("Admin seeding disabled.");

        if (userEnabled) ensureAccount(userEmail, userUsername, userRole, userPassword, resetUserPassword, false);
        else log.info("User seeding disabled.");

        if (blockedEnabled) ensureAccount(blockedEmail, blockedUsername, blockedRole, blockedPassword, resetBlockedPassword, true);
        else log.info("Blocked seeding disabled.");
    }

    private void ensureAccount(String email, String username, String role, String rawPassword, boolean resetPasswordFlag, boolean isLocked) {
        persons.findFirstByEmail(email).ifPresentOrElse(existing -> {
            boolean changed = false;

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
            p.setPassword(passwordEncoder.encode(rawPassword));
            p.setDeleted(isLocked);

            persons.save(p);
            log.warn("Default account '{}' with role {} created.", email, role);
        });
    }
}

