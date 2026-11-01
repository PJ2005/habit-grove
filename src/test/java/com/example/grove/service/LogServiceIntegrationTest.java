package com.example.grove.service;

import com.example.grove.model.*;
import com.example.grove.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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

    private void setUp() {
        user = userRepository.saveAndFlush(new AppUser("Test", "test@example.com", "STUDENT"));
        reading = habitRepository.saveAndFlush(
                new Habit(user, "Reading", HabitType.POSITIVE, 2, 5, true));
        tree = treeRepository.saveAndFlush(new Tree(user, "Oak", "OAK"));
    }

    @Test
    void loggingHabitCreatesSnapshotForThatDay() {
        setUp();
        LocalDate day = LocalDate.now().minusDays(1);

        logService.logHabit(user.getId(), reading.getId(), day, 1);

        DailySnapshot snapshot = snapshotRepository.findByTreeAndDay(tree, day).orElseThrow();
        assertEquals(2, snapshot.getPositivePoints());
        assertEquals(0, snapshot.getNegativePoints());
        assertEquals(20, snapshot.getDailyScore());
    }

    @Test
    void dailyCapStopsOneHabitFromMaxingTheScore() {
        setUp();
        LocalDate day = LocalDate.now();

        // Cap is dailyTarget x weight = 5 x 2 = 10 points
        for (int i = 0; i < 10; i++) {
            logService.logHabit(user.getId(), reading.getId(), day, 1);
        }

        DailySnapshot snapshot = snapshotRepository.findByTreeAndDay(tree, day).orElseThrow();
        assertEquals(10, snapshot.getPositivePoints());
        assertEquals(100, snapshot.getDailyScore());
    }

    @Test
    void negativeHabitLowersDailyScore() {
        setUp();
        LocalDate day = LocalDate.now();
        Habit doomscroll = habitRepository.saveAndFlush(
                new Habit(user, "Doomscrolling", HabitType.NEGATIVE, 2, 1, false));

        logService.logHabit(user.getId(), reading.getId(), day, 2);    // 4 positive
        logService.logHabit(user.getId(), doomscroll.getId(), day, 1); // 2 weight x 1 = 2 negative

        DailySnapshot snapshot = snapshotRepository.findByTreeAndDay(tree, day).orElseThrow();
        assertEquals(4, snapshot.getPositivePoints());
        assertEquals(2, snapshot.getNegativePoints());
        // 100 * (4 - 2*2) / 10 = 0
        assertEquals(0, snapshot.getDailyScore());
    }
}
