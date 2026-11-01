package com.example.grove.model;

import jakarta.persistence.*;

@Entity
@Table(name = "habit")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HabitType type;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private int dailyTarget;

    @Column(nullable = false)
    private boolean defaultHabit;

    protected Habit() {}

    public Habit(AppUser user, String name, HabitType type, int weight, int dailyTarget, boolean defaultHabit) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.weight = weight;
        this.dailyTarget = dailyTarget;
        this.defaultHabit = defaultHabit;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getName() { return name; }
    public HabitType getType() { return type; }
    public int getWeight() { return weight; }
    public int getDailyTarget() { return dailyTarget; }
    public boolean isDefaultHabit() { return defaultHabit; }
}
