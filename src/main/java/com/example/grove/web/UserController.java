package com.example.grove.web;

import com.example.grove.model.*;
import com.example.grove.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final AppUserRepository users;
    private final HabitRepository habits;
    private final TreeRepository trees;

    public UserController(AppUserRepository users, HabitRepository habits, TreeRepository trees) {
        this.users = users;
        this.habits = habits;
        this.trees = trees;
    }

    public record CreateUserRequest(String name, String email, String targetProfile) {}
    public record CreateHabitRequest(String name, HabitType type, int weight, int dailyTarget) {}
    public record HabitResponse(Long id, String name, HabitType type, int weight, int dailyTarget) {}
    public record IdResponse(Long id) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createUser(@RequestBody CreateUserRequest req) {
        AppUser user = users.save(new AppUser(req.name(), req.email(), req.targetProfile()));
        return new IdResponse(user.getId());
    }

    @PostMapping("/{userId}/habits")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse createHabit(@PathVariable Long userId, @RequestBody CreateHabitRequest req) {
        AppUser user = findUser(userId);
        Habit habit = habits.save(new Habit(user, req.name(), req.type(),
                req.weight(), req.dailyTarget(), false));
        return new IdResponse(habit.getId());
    }

    @GetMapping("/{userId}/habits")
    public List<HabitResponse> listHabits(@PathVariable Long userId) {
        return habits.findByUser(findUser(userId)).stream()
                .map(h -> new HabitResponse(h.getId(), h.getName(), h.getType(),
                        h.getWeight(), h.getDailyTarget()))
                .toList();
    }

    private AppUser findUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
