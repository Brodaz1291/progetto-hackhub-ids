package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.configs.MutableClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Reads and moves the clock of the platform. It is a development tool, not a use case: it
 * exists so that the whole life cycle of a hackathon can be shown in a single run, without
 * waiting for the real dates. Outside the development profile it does not exist.
 */
@RestController
@Profile("dev")
@RequestMapping("/api/dev/clock")
public class DevClockController {

    private static final Logger log = LoggerFactory.getLogger(DevClockController.class);

    private final Clock clock;

    public DevClockController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping
    public String getTime() {
        return LocalDateTime.now(clock).toString();
    }

    /**
     * Moves the clock to the given instant, written in the same ISO form as the app.time.fixed
     * property. Only the simulated clock can be moved: when the platform follows the system
     * time there is nothing to move.
     */
    @PutMapping
    public String setTime(@RequestParam String instant) {
        if (!(clock instanceof MutableClock mutableClock)) {
            throw new IllegalStateException(
                    "The clock follows the system time and cannot be moved: restart with app.time.mode=fixed");
        }
        LocalDateTime newTime = parseTime(instant);
        mutableClock.setInstant(newTime.atZone(clock.getZone()).toInstant());
        log.info("Clock moved to {}", newTime);
        return LocalDateTime.now(clock).toString();
    }

    private LocalDateTime parseTime(String instant) {
        try {
            return LocalDateTime.parse(instant);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid instant: " + instant + ". Expected the ISO form 2026-02-15T10:00:00");
        }
    }
}
