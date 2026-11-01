package com.example.grove.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tree_share",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tree_id", "viewer_id"}))
public class TreeShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id")
    private Tree tree;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id")
    private AppUser viewer;

    @Column(nullable = false)
    private boolean showHabitNames;

    protected TreeShare() {}

    public TreeShare(Tree tree, AppUser viewer, boolean showHabitNames) {
        this.tree = tree;
        this.viewer = viewer;
        this.showHabitNames = showHabitNames;
    }

    public void setShowHabitNames(boolean showHabitNames) { this.showHabitNames = showHabitNames; }

    public Long getId() { return id; }
    public Tree getTree() { return tree; }
    public AppUser getViewer() { return viewer; }
    public boolean isShowHabitNames() { return showHabitNames; }
}
