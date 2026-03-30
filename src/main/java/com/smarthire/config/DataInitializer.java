package com.smarthire.config;

import java.util.List;

import com.smarthire.entity.User;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByEmailIgnoreCase("recruiter@smarthire.com")
                .orElseGet(() -> createUser("Recruiter Demo", "recruiter@smarthire.com", "Recruiter@123", UserRole.RECRUITER,
                        List.of("Hiring", "Java", "Spring Boot")));
        userRepository.findByEmailIgnoreCase("candidate@smarthire.com")
                .orElseGet(() -> createUser("Candidate Demo", "candidate@smarthire.com", "Candidate@123", UserRole.CANDIDATE,
                        List.of("Java", "Spring Boot", "PostgreSQL", "Docker")));
    }

    private User createUser(String name, String email, String password, UserRole role, List<String> skills) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setVerified(true);
        user.setSkills(skills);
        return userRepository.save(user);
    }
}
