package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.model.entities.Judge;
import it.unicam.cs.hackhub.model.entities.Mentor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Notifications are ephemeral: there is no Notification entity and nothing is persisted.
 * Here the delivery is simulated with a log; this is the point where a real system would
 * send emails or push notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyStaff(Judge judge, List<Mentor> mentors) {
        String mentorUsernames = mentors.stream()
                .map(mentor -> mentor.getUser().getUsername())
                .collect(Collectors.joining(", "));
        log.info("NotificationService: notified judge {} and mentors [{}] of their assignment",
                judge.getUser().getUsername(), mentorUsernames);
    }
}
