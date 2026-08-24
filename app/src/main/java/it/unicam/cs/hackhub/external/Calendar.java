package it.unicam.cs.hackhub.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The calendar of a third party, where the calls between a mentor and a team end up as
 * events. It is not our code: the signature is the one the external system offers, and only
 * the operation the platform consumes is modelled.
 *
 * The booking is simulated in memory, where the real calendar would answer over the network.
 * What is kept from the real behaviour is the refusal, so that an answer which is not a
 * confirmation stays a case the platform has to cope with.
 *
 * The agenda lives as long as the application and nothing ever empties it: the bookings only
 * pile up, and a slot asked twice for the same people is refused the second time, even when
 * the two calls are far apart in a run.
 *
 * What is compared is the interval, not the instant: an event lasts as long as the duration it
 * was booked with, so two events of the same person collide whenever they cross, and not only
 * when they begin together. Two events that touch — one ending where the next begins — are
 * accepted.
 */
@Component
public class Calendar {

    private static final Logger log = LoggerFactory.getLogger(Calendar.class);

    private final List<Booking> bookings = new ArrayList<>();

    /**
     * One attendee held for the length of one event. Two bookings of the same person collide
     * when their intervals cross, which is what makes a calendar a calendar: an event lasts,
     * so the one that follows cannot begin before the previous is over.
     */
    private record Booking(String attendee, LocalDateTime start, LocalDateTime end) {

        private boolean overlaps(List<String> attendees, LocalDateTime otherStart, LocalDateTime otherEnd) {
            return attendees.contains(attendee)
                    && start.isBefore(otherEnd)
                    && otherStart.isBefore(end);
        }
    }

    /**
     * Books an event and returns the identifier it is filed under, or null when one of the
     * attendees is already taken at that time.
     */
    public String createEvent(String title, LocalDateTime start, int durationMinutes, List<String> attendees) {
        LocalDateTime end = start.plusMinutes(durationMinutes);
        if (bookings.stream().anyMatch(booking -> booking.overlaps(attendees, start, end))) {
            log.info("Calendar: refused \"{}\" on {}, some attendee is already busy", title, start);
            return null;
        }

        attendees.forEach(attendee -> bookings.add(new Booking(attendee, start, end)));
        String externalId = "CAL-" + UUID.randomUUID();
        log.info("Calendar: booked \"{}\" on {} for {} minutes with {} as {}",
                title, start, durationMinutes, attendees, externalId);
        return externalId;
    }
}
