package it.unicam.cs.hackhub.designPatterns;

import it.unicam.cs.hackhub.model.entities.Mentor;
import it.unicam.cs.hackhub.model.entities.Team;

import java.time.LocalDateTime;

/**
 * How the platform asks for a call to be booked, said in the words of the domain. CallService
 * depends on this interface alone: which calendar answers, and what its API looks like, stays
 * outside the domain.
 */
public interface CallScheduler {

    String bookSlot(Mentor mentor, Team team, LocalDateTime dateTime);
}
