package com.example.grove.service;

import com.example.grove.domain.TreeHealth;
import com.example.grove.model.*;
import com.example.grove.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class SharingService {

    public record DayPoint(LocalDate day, int dailyScore, double rollingScore) {}
    public record FriendTreeView(String name, String species, String status, int growthPoints,
                                 double rollingScore, String health,
                                 List<String> recentHabits, List<DayPoint> history) {}

    private final AppUserRepository users;
    private final FriendshipRepository friendships;
    private final TreeRepository trees;
    private final TreeShareRepository shares;
    private final DailySnapshotRepository snapshots;
    private final HabitLogRepository logs;

    public SharingService(AppUserRepository users, FriendshipRepository friendships,
                          TreeRepository trees, TreeShareRepository shares,
                          DailySnapshotRepository snapshots, HabitLogRepository logs) {
        this.users = users;
        this.friendships = friendships;
        this.trees = trees;
        this.shares = shares;
        this.snapshots = snapshots;
        this.logs = logs;
    }

    @Transactional
    public Friendship requestFriendship(Long fromId, Long toId) {
        if (fromId.equals(toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot befriend yourself");
        }
        AppUser from = user(fromId);
        AppUser to = user(toId);
        if (friendships.findByUserAndFriend(from, to).isPresent()
                || friendships.findByUserAndFriend(to, from).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists");
        }
        return friendships.save(new Friendship(from, to));
    }

    @Transactional
    public void acceptFriendship(Long userId, Long requesterId) {
        Friendship request = friendships.findByUserAndFriend(user(requesterId), user(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such request"));
        request.accept();
    }

    public boolean areFriends(AppUser a, AppUser b) {
        return friendships.findByUserAndFriendAndStatus(a, b, FriendStatus.ACCEPTED).isPresent()
                || friendships.findByUserAndFriendAndStatus(b, a, FriendStatus.ACCEPTED).isPresent();
    }

    /** Owner grants a friend access to their living tree, and chooses whether habit names are visible. */
    @Transactional
    public TreeShare setShare(Long ownerId, Long viewerId, boolean showHabitNames) {
        AppUser owner = user(ownerId);
        AppUser viewer = user(viewerId);
        if (!areFriends(owner, viewer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only share with friends");
        }
        Tree tree = livingTree(owner);
        TreeShare share = shares.findByTreeAndViewer(tree, viewer)
                .orElseGet(() -> new TreeShare(tree, viewer, false));
        share.setShowHabitNames(showHabitNames);
        return shares.save(share);
    }

    /** A friend's read-only view. Habit names appear only when the owner has opted in. */
    @Transactional(readOnly = true)
    public FriendTreeView viewTree(Long viewerId, Long ownerId) {
        AppUser viewer = user(viewerId);
        AppUser owner = user(ownerId);
        if (!areFriends(viewer, owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not friends with this user");
        }
        Tree tree = livingTree(owner);
        TreeShare share = shares.findByTreeAndViewer(tree, viewer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tree not shared with you"));

        List<DailySnapshot> history = snapshots.findByTreeOrderByDayAsc(tree);
        double rolling = history.isEmpty() ? 0.0 : history.get(history.size() - 1).getRollingScore();

        List<String> habitNames = List.of();
        if (share.isShowHabitNames()) {
            LocalDate today = LocalDate.now();
            habitNames = logs.findByUserAndOccurredOnBetween(owner, today.minusDays(6), today).stream()
                    .map(l -> l.getHabit().getName())
                    .distinct()
                    .toList();
        }

        return new FriendTreeView(
                tree.getName(), tree.getSpecies(), tree.getStatus().name(), tree.getGrowthPoints(),
                rolling, TreeHealth.fromRolling(rolling).name(), habitNames,
                history.stream()
                        .map(s -> new DayPoint(s.getDay(), s.getDailyScore(), s.getRollingScore()))
                        .toList());
    }

    private AppUser user(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Tree livingTree(AppUser owner) {
        return trees.findFirstByOwnerAndStatus(owner, TreeStatus.ALIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No living tree"));
    }
}
