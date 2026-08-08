package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.model.entities.Judge;
import it.unicam.cs.hackhub.model.entities.Mentor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gli avvisi sono effimeri: non esiste un'entità Notification e nulla viene persistito.
 * Qui l'invio è simulato da un log; è il punto in cui un sistema reale spedirebbe
 * email o notifiche push.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyStaff(Judge judge, List<Mentor> mentors) {
        String mentorUsernames = mentors.stream()
                .map(mentor -> mentor.getUser().getUsername())
                .collect(Collectors.joining(", "));
        log.info("Assegnazione allo staff notificata: giudice {}, mentori [{}]",
                judge.getUser().getUsername(), mentorUsernames);
    }
}
