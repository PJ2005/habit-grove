package com.example.grove.repository;

import com.example.grove.model.AppUser;
import com.example.grove.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUser(AppUser user);
}
