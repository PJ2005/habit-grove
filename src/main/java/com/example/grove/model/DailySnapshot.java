package com.example.grove.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_snapshot",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tree_id", "snapshot_day"}))
public class DailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id")
    private Tree tree;

    @Column(name = "snapshot_day", nullable = false)
    private LocalDate day;
    
    @Column(nullable = false)
    private int positivePoints;

    @Column(nullable = false)
    private int negativePoints;

    @Column(nullable = false)
    private int dailyScore;

    @Column(nullable = false)
    private double rollingScore;

    protected DailySnapshot() {}

    public DailySnapshot(Tree tree, LocalDate day, int positivePoints, int negativePoints,
                         int dailyScore, double rollingScore) {
        this.tree = tree;
        this.day = day;
        this.positivePoints = positivePoints;
        this.negativePoints = negativePoints;
        this.dailyScore = dailyScore;
        this.rollingScore = rollingScore;
    }

    public Long getId() { return id; }
    public Tree getTree() { return tree; }
    public LocalDate getDay() { return day; }
    public int getPositivePoints() { return positivePoints; }
    public int getNegativePoints() { return negativePoints; }
    public int getDailyScore() { return dailyScore; }
    public double getRollingScore() { return rollingScore; }
}
