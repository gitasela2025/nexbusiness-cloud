package com.nexlora.nexbusiness.config;

import com.nexlora.nexbusiness.entity.User;
import com.nexlora.nexbusiness.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
                       @Value("${admin.email:}") String email,
                       @Value("${admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || password.length() < 8) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set (8+ chars): no admin seeded.");
            return;
        }
        User admin = users.findByEmail(email).orElseGet(User::new);
        admin.setEmail(email);
        if (admin.getName() == null) admin.setName("Admin");
        admin.setRole(User.Role.ADMIN);
        admin.setPasswordHash(encoder.encode(password));
        users.save(admin);
        log.info("Admin account ready: {}", email);
    }
}
