package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.ConflictException;
import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.enums.RegistrationState;
import it.unicam.cs.hackhub.model.repositories.HackathonRepository;
import it.unicam.cs.hackhub.model.repositories.RegistrationRepository;
import it.unicam.cs.hackhub.model.repositories.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;
    private final HackathonLifecycle hackathonLifecycle;
    private final Clock clock;

    public RegistrationService(RegistrationRepository registrationRepository,
                               HackathonRepository hackathonRepository,
                               TeamRepository teamRepository,
                               NotificationService notificationService,
                               HackathonLifecycle hackathonLifecycle,
                               Clock clock) {
        this.registrationRepository = registrationRepository;
        this.hackathonRepository = hackathonRepository;
        this.teamRepository = teamRepository;
        this.notificationService = notificationService;
        this.hackathonLifecycle = hackathonLifecycle;
        this.clock = clock;
    }

    /**
     * Enrols a team in a hackathon still open to registrations and notifies it.
     */
    @Transactional
    public Registration registerTeam(Long hackathonId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found: " + teamId));
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        hackathonLifecycle.refreshState(hackathon);
        if (isTeamBusy(team)) {
            throw new ConflictException("Team " + teamId + " is already taking part in a hackathon");
        }
        if (!checkRegistrationConditions(team, hackathon)) {
            throw new ValidationException("Registration conditions not satisfied for hackathon " + hackathonId);
        }

        Registration registration = new Registration(team, hackathon,
                LocalDateTime.now(clock), RegistrationState.REGISTERED);
        // the hackathon is managed inside the transaction, so the registration is added to its
        // list to keep the in-memory graph coherent for whoever reads it next
        hackathon.getRegistrations().add(registration);
        // saving the transient registration is a persist, and IDENTITY assigns the id at once.
        // Saving the hackathon instead would be a merge: through the cascade it persists a copy
        // of the registration and leaves this instance without an id
        Registration savedRegistration = registrationRepository.save(registration);

        notificationService.notifyTeam(team);
        return savedRegistration;
    }

    /**
     * A team takes part in one hackathon at a time. The query looks at the phase of the
     * hackathon only: a disqualified team stays busy until the event is over, because the
     * disqualification is a sanction, not a way out.
     */
    private boolean isTeamBusy(Team team) {
        return registrationRepository.findActiveByTeam(team).isPresent();
    }

    private boolean checkRegistrationConditions(Team team, Hackathon hackathon) {
        return hackathon.getState() == HackathonState.REGISTRATION
                && LocalDateTime.now(clock).isBefore(hackathon.getRegistrationDeadline())
                && team.getMembers().size() <= hackathon.getMaxTeamSize();
    }

    /**
     * Disqualifies the registration of a team, recording the reason, and notifies the team.
     */
    @Transactional
    public void disqualifyTeam(Long registrationId, String reason) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found: " + registrationId));
        if (!checkHackathonNotConcluded(registration.getHackathon())) {
            throw new ValidationException("A team cannot be disqualified from a concluded hackathon");
        }
        if (!checkNotAlreadyDisqualified(registration)) {
            throw new ConflictException("Registration " + registrationId + " is already disqualified");
        }
        if (!checkReason(reason)) {
            throw new ValidationException("Reason of the disqualification is required");
        }

        applyDisqualification(registration, reason);
        registrationRepository.save(registration);

        notificationService.notifyTeam(registration.getTeam());
    }

    /**
     * Once the event is over the disqualification has no object left: the result is settled
     * and the winner already proclaimed.
     */
    private boolean checkHackathonNotConcluded(Hackathon hackathon) {
        return hackathon.getState() != HackathonState.CONCLUDED;
    }

    private boolean checkNotAlreadyDisqualified(Registration registration) {
        return registration.getState() != RegistrationState.DISQUALIFIED;
    }

    /**
     * The disqualification cannot be revoked and its reason is kept in the history of the
     * event: an empty motivation would leave the exclusion of a team unexplained forever.
     *
     * It is the last check of disqualifyTeam on purpose: if the hackathon is concluded or the
     * team is already disqualified the operation is impossible whatever the motivation, and
     * complaining about the reason first would point at the wrong problem.
     */
    private boolean checkReason(String reason) {
        return StringUtils.hasText(reason);
    }

    /**
     * The disqualification marks the registration, it does not remove it: submissions,
     * evaluations and the history of the event survive the exclusion of the team.
     */
    private void applyDisqualification(Registration registration, String reason) {
        registration.setState(RegistrationState.DISQUALIFIED);
        registration.setDisqualificationReason(reason);
    }

    /**
     * Returns the teams enrolled in a hackathon. It is the list the organizer reads before
     * disqualifying a team and the mentor before reporting one: both operations work on a
     * registration, and this is the only place where its identifier can be found.
     *
     * The disqualified registrations stay in the list, with their state: the exclusion of a team
     * is part of what the organizer is looking at.
     */
    public List<Registration> getRegistrations(Long hackathonId) {
        return registrationRepository.findByHackathonId(hackathonId);
    }
}
