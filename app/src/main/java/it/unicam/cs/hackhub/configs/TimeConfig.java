package it.unicam.cs.hackhub.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The time source of the platform. The services read the current instant from this bean instead
 * of calling LocalDateTime.now(), so that the demonstration can move the clock and let the
 * hackathons change state without waiting for the real dates.
 *
 * The bean is exposed as a java.time.Clock: the services depend on the standard abstraction and
 * ignore whether time is real or simulated.
 */
@Configuration
public class TimeConfig {

    private static final Logger log = LoggerFactory.getLogger(TimeConfig.class);

    @Bean
    public Clock clock(@Value("${app.time.mode}") String mode,
                       @Value("${app.time.fixed}") String fixedTime) {
        if ("system".equalsIgnoreCase(mode)) {
            Clock clock = Clock.systemDefaultZone();
            log.info("Clock mode=system, current time {}", LocalDateTime.now(clock));
            return clock;
        }
        if (!"fixed".equalsIgnoreCase(mode)) {
            throw new IllegalStateException(
                    "Unknown app.time.mode: " + mode + ". Expected \"fixed\" or \"system\"");
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime startTime = LocalDateTime.parse(fixedTime);
        log.info("Clock mode=fixed, starting time {}", startTime);
        return new MutableClock(startTime.atZone(zone).toInstant(), zone);
    }
}
