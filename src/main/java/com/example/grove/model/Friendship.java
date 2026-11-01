package com.example.grove.model;

import jakarta.persistence.*;

@Entity
@Table(name = "friendship",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"}))
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who sent the request. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    /** The user who receives the request. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id")
    private AppUser friend;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendStatus status = FriendStatus.PENDING;

    protected Friendship() {}

    public Friendship(AppUser user, AppUser friend) {
        this.user = user;
        this.friend = friend;
    }

    public void accept() { this.status = FriendStatus.ACCEPTED; }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public AppUser getFriend() { return friend; }
    public FriendStatus getStatus() { return status; }
}
