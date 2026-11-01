package com.example.grove.service;

import com.example.grove.model.*;
import com.example.grove.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class LogService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository logRepository;
    private final TreeRepository treeRepository;
    private final SnapshotService snapshotService;

    public LogService(HabitRepository habitRepository,
                      HabitLogRepository logRepository,
                      TreeRepository treeRepository,
                      SnapshotService snapshotService) {
        this.habitRepository = habitRepository;
        this.logRepository = logRepository;
        this.treeRepository = treeRepository;
        this.snapshotService = snapshotService;
    }

    /**
     * Logs an action for a habit on a given day (can be in the past),
     * then rebuilds the tree's scores from that day onward.
     */
    @Transactional
    public HabitLog logHabit(Long habitId, LocalDate occurredOn, int quantity) {
        Habit habit = habitRepository.findById(habitId)
                        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Habit not found: " + habitId));

        HabitLog log = logRepository.save(
                new HabitLog(habit, habit.getUser(), occurredOn, quantity));

        Tree tree = treeRepository
                .findFirstByOwnerAndStatus(habit.getUser(), TreeStatus.ALIVE)
                .orElseThrow(() -> new IllegalStateException("No living tree for user"));

        snapshotService.recalculateFrom(tree, occurredOn);
        return log;
    }
}
