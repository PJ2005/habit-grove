package com.example.grove.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "habit_log")
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id")
    private Habit habit;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private LocalDate occurredOn;

    @Column(nullable = false, updatable = false)
    private Instant loggedAt = Instant.now();

    @Column(nullable = false)
    private int quantity;

    protected HabitLog() {}

    public HabitLog(Habit habit, AppUser user, LocalDate occurredOn, int quantity) {
        this.habit = habit;
        this.user = user;
        this.occurredOn = occurredOn;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Habit getHabit() { return habit; }
    public AppUser getUser() { return user; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public Instant getLoggedAt() { return loggedAt; }
    public int getQuantity() { return quantity; }
}
