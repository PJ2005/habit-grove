package com.example.grove.repository;

import com.example.grove.model.DailySnapshot;
import com.example.grove.model.Tree;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySnapshotRepository extends JpaRepository<DailySnapshot, Long> {
    List<DailySnapshot> findByTreeOrderByDayAsc(Tree tree);
    Optional<DailySnapshot> findByTreeAndDay(Tree tree, LocalDate day);
}
