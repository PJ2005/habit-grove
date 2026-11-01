package com.example.grove.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String targetProfile;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {}

    public AppUser(String name, String email, String targetProfile) {
        this.name = name;
        this.email = email;
        this.targetProfile = targetProfile;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getTargetProfile() { return targetProfile; }
    public Instant getCreatedAt() { return createdAt; }
}
