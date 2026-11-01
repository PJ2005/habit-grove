package com.example.grove.service;

import com.example.grove.model.*;
import com.example.grove.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LogServiceIntegrationTest {

    @Autowired private AppUserRepository userRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private DailySnapshotRepository snapshotRepository;
    @Autowired private LogService logService;

    private AppUser user;
    private Habit reading;
    private Tree tree;

    @Test
    void loggingHabitCreatesSnapshotForThatDay() {
        setUp();
        LocalDate day = LocalDate.now().minusDays(1);

        logService.logHabit(reading.getId(), day, 1);

        List<DailySnapshot> snapshots = snapshotRepository.findByTreeOrderByDayAsc(tree);
        DailySnapshot snapshot = snapshots.stream()
                .filter(s -> s.getDay().equals(day))
                .findFirst()
                .orElseThrow();

        // Reading has weight 2, quantity 1 → 2 positive points → daily score 20
        assertEquals(2, snapshot.getPositivePoints());
        assertEquals(0, snapshot.getNegativePoints());
        assertEquals(20, snapshot.getDailyScore());
    }

    @Test
    void dailyCapStopsOneHabitFromMaxingTheScore() {
        setUp();
        LocalDate day = LocalDate.now();

        // 10 logs of reading (weight 2) would be 20 points,
        // but the cap is dailyTarget × weight = 5 × 2 = 10
        for (int i = 0; i < 10; i++) {
            logService.logHabit(reading.getId(), day, 1);
        }

        DailySnapshot snapshot = snapshotRepository.findByTreeAndDay(tree, day).orElseThrow();
        assertEquals(10, snapshot.getPositivePoints());
        assertEquals(100, snapshot.getDailyScore());
    }

    @Test
    void negativeHabitLowersDailyScore() {
        setUp();
        LocalDate day = LocalDate.now();
        Habit doomscroll = habitRepository.save(
                new Habit(user, "Doomscrolling", HabitType.NEGATIVE, 2, 1, false));

        logService.logHabit(reading.getId(), day, 2);   // 4 positive
        logService.logHabit(doomscroll.getId(), day, 1); // 4 negative, counts double

        DailySnapshot snapshot = snapshotRepository.findByTreeAndDay(tree, day).orElseThrow();
        assertEquals(4, snapshot.getPositivePoints());
        assertEquals(2, snapshot.getNegativePoints());
        // dailyScore = 100 * (4 - 2*2) / 10 = 0, clamped at 0
        assertEquals(0, snapshot.getDailyScore());
    }

    private void setUp() {
        user = userRepository.save(new AppUser("Test", "test@example.com", "STUDENT"));
        reading = habitRepository.save(
                new Habit(user, "Reading", HabitType.POSITIVE, 2, 5, true));
        tree = treeRepository.save(new Tree(user, "Oak", "OAK"));
    }
}
