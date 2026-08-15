package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.model.entities.Judge;
import it.unicam.cs.hackhub.model.entities.Mentor;
import it.unicam.cs.hackhub.model.entities.Organizer;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.entities.User;
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

    public void notifyUser(User user) {
        log.info("NotificationService: notified user {} of the invitation received",
                user.getUsername());
    }

    /**
     * The same notice serves assignment and removal, so the wording stays neutral.
     */
    public void notifyMentor(Mentor mentor) {
        log.info("NotificationService: notified mentor {} of the change to their role",
                mentor.getUser().getUsername());
    }

    /**
     * The same notice serves assignment and revocation, so the wording stays neutral.
     */
    public void notifyJudge(Judge judge) {
        log.info("NotificationService: notified judge {} of the change to their role",
                judge.getUser().getUsername());
    }

    /**
     * The same notice serves registration and disqualification, so the wording stays neutral.
     */
    public void notifyTeam(Team team) {
        log.info("NotificationService: notified team {} of the change to their registration",
                team.getName());
    }

    public void notifyOrganizer(Organizer organizer) {
        log.info("NotificationService: notified organizer {} of the report received",
                organizer.getUser().getUsername());
    }
}
