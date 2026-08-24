package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.repositories.HackathonRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * The phases a hackathon goes through on its own, without anyone commanding them: the closing
 * of the registrations and the end of the submissions happen on their dates.
 *
 * The rule lives here, and not inside HackathonService, because it belongs to whoever reads a
 * hackathon rather than to whoever manages one: the services that check the phase before
 * accepting a registration, a submission or a support request need this rule alone, and
 * depending on the whole HackathonService to reach it would tie them to the builder and to
 * the participations they have nothing to do with.
 */
@Component
public class HackathonLifecycle {

    private final HackathonRepository hackathonRepository;
    private final Clock clock;

    public HackathonLifecycle(HackathonRepository hackathonRepository, Clock clock) {
        this.hackathonRepository = hackathonRepository;
        this.clock = clock;
    }

    /**
     * Brings the phase of a hackathon up to the current time, and saves it if it has changed.
     * The transitions are applied on reading, so a simple consultation can write: it is the
     * price of having no scheduler, and whoever calls this method has to expect it.
     *
     * The registrations close on the deadline, and it is that date, not the start of the event,
     * that ends the registration phase: from there the teams that take part are settled, so the
     * hackathon is no longer open even though it has not begun yet. The days in between are
     * part of the running phase, where the operations of the event are refused until the start
     * date: whoever guards them checks the date as well as the phase.
     *
     * The two transitions are applied one after the other, not as alternatives: if the clock
     * moves far enough ahead, a hackathon still open to registrations reaches the evaluation
     * in a single refresh. They only move forward, so a clock set back does not bring a
     * hackathon back to a phase it has already left, where the submissions it has collected
     * would not make sense. The concluded phase is terminal and is never touched.
     */
    public void refreshState(Hackathon hackathon) {
        LocalDateTime now = LocalDateTime.now(clock);
        HackathonState previousState = hackathon.getState();
        if (hackathon.getState() == HackathonState.REGISTRATION
                && !now.isBefore(hackathon.getRegistrationDeadline())) {
            hackathon.setState(HackathonState.RUNNING);
        }
        if (hackathon.getState() == HackathonState.RUNNING
                && !now.isBefore(hackathon.getEndDate())) {
            hackathon.setState(HackathonState.EVALUATION);
        }
        if (hackathon.getState() != previousState) {
            hackathonRepository.save(hackathon);
        }
    }
}
