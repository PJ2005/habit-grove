package com.example.grove.web;

import com.example.grove.domain.TreeHealth;
import com.example.grove.model.*;
import com.example.grove.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.time.ZoneId;

@RestController
@RequestMapping("/users/{userId}/tree")
public class TreeController {

    private final AppUserRepository users;
    private final TreeRepository trees;
    private final DailySnapshotRepository snapshots;

    public TreeController(AppUserRepository users, TreeRepository trees,
                          DailySnapshotRepository snapshots) {
        this.users = users;
        this.trees = trees;
        this.snapshots = snapshots;
    }

    public record CreateTreeRequest(String name, String species) {}
    public record IdResponse(Long id) {}
    public record TreeResponse(Long id, String name, String species, int growthPoints,
                               String status, LocalDate today, Integer todayScore,
                               Double rollingScore, String health) {}
    public record HistoryPoint(LocalDate day, int positivePoints, int negativePoints,
                               int dailyScore, double rollingScore, String health) {}
    public record MemorialEntry(Long id, String name, String species, String status,
                               int growthPoints, LocalDate createdOn, LocalDate diedOn) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createTree(@PathVariable Long userId, @RequestBody CreateTreeRequest req) {
        AppUser owner = findUser(userId);
        if (trees.findFirstByOwnerAndStatus(owner, TreeStatus.ALIVE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User already has a living tree");
        }
        Tree tree = trees.save(new Tree(owner, req.name(), req.species()));
        return new IdResponse(tree.getId());
    }

    @GetMapping
    public TreeResponse currentTree(@PathVariable Long userId) {
        Tree tree = findLivingTree(userId);
        DailySnapshot latest = snapshots.findByTreeOrderByDayAsc(tree).stream()
                .reduce((first, second) -> second)
                .orElse(null);

        Integer todayScore = null;
        Double rolling = null;
        String health = TreeHealth.DEAD.name();
        if (latest != null) {
            todayScore = latest.getDailyScore();
            rolling = latest.getRollingScore();
            health = TreeHealth.fromRolling(rolling).name();
        }
        return new TreeResponse(tree.getId(), tree.getName(), tree.getSpecies(),
                tree.getGrowthPoints(), tree.getStatus().name(), LocalDate.now(),
                todayScore, rolling, health);
    }

    @GetMapping("/history")
    public List<HistoryPoint> history(@PathVariable Long userId) {
        Tree tree = findLivingTree(userId);
        return snapshots.findByTreeOrderByDayAsc(tree).stream()
                .map(s -> new HistoryPoint(s.getDay(), s.getPositivePoints(), s.getNegativePoints(),
                        s.getDailyScore(), s.getRollingScore(),
                        TreeHealth.fromRolling(s.getRollingScore()).name()))
                .toList();
    }
    
    @GetMapping("/all")
    public List<MemorialEntry> allTrees(@PathVariable Long userId) {
        AppUser owner = findUser(userId);
        return trees.findByOwnerOrderByCreatedAtAsc(owner).stream()
                .map(t -> new MemorialEntry(
                        t.getId(), t.getName(), t.getSpecies(), t.getStatus().name(),
                        t.getGrowthPoints(),
                        LocalDate.ofInstant(t.getCreatedAt(), ZoneId.systemDefault()),
                        t.getDiedAt() == null ? null
                                : LocalDate.ofInstant(t.getDiedAt(), ZoneId.systemDefault())))
                .toList();
    }

    private AppUser findUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Tree findLivingTree(Long userId) {
        return trees.findFirstByOwnerAndStatus(findUser(userId), TreeStatus.ALIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No living tree"));
    }
}
