package com.example.Buoi3.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;
    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(name = "password", length = 250, nullable = false)
    private String password;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Column(name = "email", length = 50, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 250)
    private String address;

    @Column(name = "reward_points")
    @Builder.Default
    private Integer rewardPoints = 0;

    @Column(name = "birth_date")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate birthDate;

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
    }
}
