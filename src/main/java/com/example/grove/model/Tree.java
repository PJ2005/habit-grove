package com.example.grove.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tree")
public class Tree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    @Column(nullable = false)
    private int growthPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TreeStatus status = TreeStatus.ALIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant diedAt;

    protected Tree() {}

    public Tree(AppUser owner, String name, String species) {
        this.owner = owner;
        this.name = name;
        this.species = species;
    }

    public void addGrowthPoints(int points) { this.growthPoints += points; }
    
    public void setGrowthPoints(int growthPoints) {
        this.growthPoints = growthPoints;
    }

    public void die() {
        this.status = TreeStatus.DEAD;
        this.diedAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getName() { return name; }
    public String getSpecies() { return species; }
    public int getGrowthPoints() { return growthPoints; }
    public TreeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDiedAt() { return diedAt; }
}
