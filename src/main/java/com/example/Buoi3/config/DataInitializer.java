package com.example.Buoi3.config;

import com.example.Buoi3.Entity.Role;
import com.example.Buoi3.Entity.User;
import com.example.Buoi3.repository.RoleRepository;
import com.example.Buoi3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ADMIN");
            role.setDescription("Quản trị viên");
            return roleRepository.save(role);
        });

        Role managerRole = roleRepository.findByName("MANAGER").orElseGet(() -> {
            Role role = new Role();
            role.setName("MANAGER");
            role.setDescription("Quản lý sản phẩm");
            return roleRepository.save(role);
        });

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role role = new Role();
            role.setName("USER");
            role.setDescription("Người dùng thường");
            return roleRepository.save(role);
        });

        // Tạo tài khoản admin mặc định nếu chưa có
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setEmail("admin@tgdd.com");
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
        }

        // Tạo tài khoản manager mặc định nếu chưa có
        if (userRepository.findByUsername("manager").isEmpty()) {
            User manager = new User();
            manager.setUsername("manager");
            manager.setPassword(passwordEncoder.encode("123456"));
            manager.setEmail("manager@tgdd.com");
            manager.getRoles().add(managerRole);
            userRepository.save(manager);
        }

        // Tạo tài khoản user mặc định nếu chưa có
        if (userRepository.findByUsername("user").isEmpty()) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setEmail("user@tgdd.com");
            user.getRoles().add(userRole);
            userRepository.save(user);
        }
    }
}
