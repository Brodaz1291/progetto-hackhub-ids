package it.unicam.cs.hackhub.designPatterns;

import it.unicam.cs.hackhub.external.Calendar;
import it.unicam.cs.hackhub.model.entities.Mentor;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.entities.TeamMember;
import it.unicam.cs.hackhub.model.entities.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Translates a booking between the two sides: the domain asks for a slot between a mentor and
 * a team, the calendar wants a title, a duration and a list of attendees. The distance
 * between bookSlot and createEvent is what this class exists to cover.
 */
@Component
public class CalendarAdapter implements CallScheduler {

    /**
     * The domain does not say how long a call lasts, while the calendar demands a duration:
     * the adapter is the one place where the missing half of the translation can live.
     */
    private static final int CALL_DURATION_MINUTES = 30;

    private final Calendar calendar;

    public CalendarAdapter(Calendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public String bookSlot(Mentor mentor, Team team, LocalDateTime dateTime) {
        String title = "Mentoring call: " + mentor.getUser().getUsername() + " - " + team.getName();
        List<String> attendees = Stream.concat(
                        Stream.of(mentor.getUser()),
                        team.getMembers().stream().map(TeamMember::getUser))
                .map(User::getUsername)
                .toList();

        String externalId = calendar.createEvent(title, dateTime, CALL_DURATION_MINUTES, attendees);
        if (externalId == null) {
            throw new IllegalArgumentException("The calendar has no slot free on " + dateTime);
        }
        return externalId;
    }
}
