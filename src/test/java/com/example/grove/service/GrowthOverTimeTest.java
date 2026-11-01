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
class GrowthOverTimeTest {

    @Autowired private AppUserRepository userRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private TreeRepository treeRepository;
    @Autowired private DailySnapshotRepository snapshotRepository;
    @Autowired private LogService logService;

    @Test
    void growthAndRollingBuildUpAcrossDays() {
        AppUser user = userRepository.saveAndFlush(
                new AppUser("Growth", "growth@example.com", "STUDENT"));
        Habit reading = habitRepository.saveAndFlush(
                new Habit(user, "Reading", HabitType.POSITIVE, 2, 5, true));
        Tree tree = treeRepository.saveAndFlush(new Tree(user, "Oak", "OAK"));

        LocalDate start = LocalDate.now().minusDays(2);
        // Day 1, day 2, day 3: each day one reading (2 points, daily score 20)
        for (int i = 0; i < 3; i++) {
            logService.logHabit(user.getId(), reading.getId(), start.plusDays(i), 1);
        }

        DailySnapshot last = snapshotRepository.findByTreeAndDay(tree, LocalDate.now()).orElseThrow();
        // Growth is the running total: 3 days x 2 points = 6
        assertEquals(6, treeRepository.findById(tree.getId()).orElseThrow().getGrowthPoints());
        // Rolling: 6.0, then 0.3*20 + 0.7*6 = 10.2, then 0.3*20 + 0.7*10.2 = 13.14
        assertEquals(13.14, last.getRollingScore(), 0.01);
    }
}
