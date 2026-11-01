package com.example.grove.web;

import com.example.grove.service.LogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/users/{userId}/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /** occurredOn is optional; omit it to log for today. Past dates are allowed. */
    public record LogRequest(Long habitId, LocalDate occurredOn, int quantity) {}
    public record LogResponse(Long id, LocalDate occurredOn, int quantity) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogResponse log(@PathVariable Long userId, @RequestBody LogRequest req) {
        LocalDate day = req.occurredOn() != null ? req.occurredOn() : LocalDate.now();
        var log = logService.logHabit(req.habitId(), day, req.quantity());
        return new LogResponse(log.getId(), log.getOccurredOn(), log.getQuantity());
    }
}
