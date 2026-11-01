package com.example.grove.repository;

import com.example.grove.model.AppUser;
import com.example.grove.model.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    List<HabitLog> findByUserAndOccurredOn(AppUser user, LocalDate day);
    List<HabitLog> findByUserAndOccurredOnBetween(AppUser user, LocalDate from, LocalDate to);
}
