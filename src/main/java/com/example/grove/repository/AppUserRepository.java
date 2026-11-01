package com.example.grove.repository;

import com.example.grove.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}

java.util.Optional<AppUser> findByEmail(String email);
