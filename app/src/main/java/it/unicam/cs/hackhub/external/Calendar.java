package it.unicam.cs.hackhub.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The third party calendar the calls are booked on. It is not our code: the signature is the
 * one the external system offers, and only the operation the platform consumes is modelled.
 *
 * This is a stub: the booking is simulated in memory, where a real system would answer over
 * the network. What is kept from the real behaviour is the refusal, so that an answer which
 * is not a confirmation stays a case the platform has to cope with.
 *
 * The agenda lives as long as the application and nothing ever empties it: the set of the
 * busy slots only grows, and asking twice for the same slot with the same people is refused
 * the second time, even when the two calls are far apart in a run.
 */
@Component
public class Calendar {

    private static final Logger log = LoggerFactory.getLogger(Calendar.class);

    private final Set<String> busySlots = new HashSet<>();

    /**
     * Books an event and returns the identifier it is filed under, or null when one of the
     * attendees is already taken at that time.
     */
    public String createEvent(String title, LocalDateTime start, int durationMinutes, List<String> attendees) {
        List<String> requestedSlots = attendees.stream()
                .map(attendee -> attendee + "@" + start)
                .toList();
        if (requestedSlots.stream().anyMatch(busySlots::contains)) {
            log.info("Calendar: refused \"{}\" on {}, some attendee is already busy", title, start);
            return null;
        }

        busySlots.addAll(requestedSlots);
        String externalId = "CAL-" + UUID.randomUUID();
        log.info("Calendar: booked \"{}\" on {} for {} minutes with {} as {}",
                title, start, durationMinutes, attendees, externalId);
        return externalId;
    }
}
