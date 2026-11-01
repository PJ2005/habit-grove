package com.example.grove.service;

import com.example.grove.domain.ScoringRules;
import com.example.grove.model.*;
import com.example.grove.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

import java.time.ZoneId;
import java.util.ArrayList;

@Service
public class SnapshotService {

    private final HabitLogRepository logRepository;
    private final DailySnapshotRepository snapshotRepository;
    private final TreeRepository treeRepository;

    public SnapshotService(HabitLogRepository logRepository,
                           DailySnapshotRepository snapshotRepository,
                           TreeRepository treeRepository) {
        this.logRepository = logRepository;
        this.snapshotRepository = snapshotRepository;
        this.treeRepository = treeRepository;
    }

    /**
     * Called after a log is saved. Rebuilds the snapshot for the log's day,
     * then recalculates rolling scores from that day onward.
     */
    @Transactional
    public void recalculateFrom(Tree tree, LocalDate fromDay) {
        LocalDate today = LocalDate.now();
        AppUser owner = tree.getOwner();

        List<HabitLog> logs = logRepository.findByUserAndOccurredOnBetween(owner, fromDay, today);
        Map<LocalDate, List<HabitLog>> byDay = logs.stream()
                .collect(Collectors.groupingBy(HabitLog::getOccurredOn));

        List<DailySnapshot> existing = snapshotRepository.findByTreeOrderByDayAsc(tree);

        // Rolling score carried in from the last snapshot before fromDay
        double rolling = existing.stream()
                .filter(s -> s.getDay().isBefore(fromDay))
                .reduce((first, second) -> second)
                .map(DailySnapshot::getRollingScore)
                .orElse(0.0);

        // Remove snapshots from fromDay onward, then flush so the deletes
        // reach the database before the rebuilt rows are inserted
        List<DailySnapshot> stale = existing.stream()
                .filter(s -> !s.getDay().isBefore(fromDay))
                .toList();
        snapshotRepository.deleteAll(stale);
        snapshotRepository.flush();

        for (LocalDate day = fromDay; !day.isAfter(today); day = day.plusDays(1)) {
            List<HabitLog> dayLogs = byDay.getOrDefault(day, List.of());

            // Sum points per habit first, so the daily cap applies per habit
            Map<Habit, Integer> perHabit = new HashMap<>();
            for (HabitLog entry : dayLogs) {
                perHabit.merge(entry.getHabit(),
                        entry.getHabit().getWeight() * entry.getQuantity(), Integer::sum);
            }

            int positive = 0;
            int negative = 0;
            for (Map.Entry<Habit, Integer> e : perHabit.entrySet()) {
                Habit habit = e.getKey();
                int points = e.getValue();
                if (habit.getType() == HabitType.POSITIVE) {
                    positive += Math.min(points, habit.getDailyTarget() * habit.getWeight());
                } else {
                    negative += points;
                }
            }

            int daily = ScoringRules.dailyScore(positive, negative);
            rolling = ScoringRules.rolling(rolling, daily);

            snapshotRepository.save(new DailySnapshot(tree, day, positive, negative, daily, rolling));
        }
      applyLifecycle(tree, today);
    }
    
    private static final int DEATH_STREAK_DAYS = 7;
    private static final double DEATH_THRESHOLD = 10.0;

        /**
     * Growth is the running total of positive points.
     * A tree dies after a streak of low days ending today. Days before the
     * tree was created never count toward that streak.
     */
    private void applyLifecycle(Tree tree, LocalDate today) {
        List<DailySnapshot> all = snapshotRepository.findByTreeOrderByDayAsc(tree);

        int growth = all.stream().mapToInt(DailySnapshot::getPositivePoints).sum();
        tree.setGrowthPoints(growth);

        LocalDate createdOn = LocalDate.ofInstant(tree.getCreatedAt(), ZoneId.systemDefault());
        Map<LocalDate, DailySnapshot> byDate = all.stream()
                .collect(Collectors.toMap(DailySnapshot::getDay, s -> s));

        // Collect rolling scores for the last 7 days, newest first
        List<Double> recent = new ArrayList<>();
        for (int i = 0; i < ScoringRules.DEATH_STREAK_DAYS; i++) {
            LocalDate day = today.minusDays(i);
            if (day.isBefore(createdOn)) break;
            DailySnapshot s = byDate.get(day);
            if (s == null) break;
            recent.add(s.getRollingScore());
        }

        if (ScoringRules.isDeathStreak(recent)) {
            tree.die();
        }
    }
}
