package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.SupportRequest;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.enums.SupportRequestState;
import it.unicam.cs.hackhub.model.repositories.RegistrationRepository;
import it.unicam.cs.hackhub.model.repositories.SupportRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final RegistrationRepository registrationRepository;
    private final HackathonLifecycle hackathonLifecycle;
    private final Clock clock;

    public SupportRequestService(SupportRequestRepository supportRequestRepository,
                                 RegistrationRepository registrationRepository,
                                 HackathonLifecycle hackathonLifecycle,
                                 Clock clock) {
        this.supportRequestRepository = supportRequestRepository;
        this.registrationRepository = registrationRepository;
        this.hackathonLifecycle = hackathonLifecycle;
        this.clock = clock;
    }

    /**
     * Registers a support request of a team. The request stays pending until a mentor takes
     * it over by scheduling a call.
     */
    public SupportRequest sendSupportRequest(Long registrationId, String description) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found: " + registrationId));
        hackathonLifecycle.refreshState(registration.getHackathon());
        if (!checkHackathonIsRunning(registration.getHackathon())) {
            throw new ValidationException("Support is available only between the start and the end of the hackathon");
        }
        if (!checkDescription(description)) {
            throw new ValidationException("Description of the support request is required");
        }

        SupportRequest supportRequest = new SupportRequest(registration, description, SupportRequestState.PENDING);
        return supportRequestRepository.save(supportRequest);
    }

    /**
     * Support belongs to the event in progress: before it starts there is nothing to ask help
     * about, once it is over the event is closed.
     *
     * The phase alone is not enough: the running phase opens when the registrations close,
     * which is days before the event begins, so the start date is checked as well.
     */
    private boolean checkHackathonIsRunning(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.RUNNING
                && !LocalDateTime.now(clock).isBefore(hackathon.getStartDate());
    }

    private boolean checkDescription(String description) {
        return StringUtils.hasText(description);
    }

    /**
     * Returns the requests of a hackathon that no mentor has taken over yet.
     */
    public List<SupportRequest> getPendingRequests(Long hackathonId) {
        return supportRequestRepository.findPendingByHackathonId(hackathonId);
    }

    /**
     * Returns all the requests sent by a team, whatever their state.
     */
    public List<SupportRequest> getTeamRequests(Long registrationId) {
        return supportRequestRepository.findByRegistrationId(registrationId);
    }

    public SupportRequest getSupportRequest(Long supportRequestId) {
        return supportRequestRepository.findById(supportRequestId)
                .orElseThrow(() -> new NotFoundException("Support request not found: " + supportRequestId));
    }

    /**
     * Marks a request as handled. Differently from the other state changes of the platform
     * this method is public, because the caller is CallService: the request is taken over
     * when a mentor schedules the call. The saving happens here since the caller cannot know
     * whether this service still has to persist something.
     */
    public void markHandled(SupportRequest supportRequest) {
        supportRequest.setStatus(SupportRequestState.HANDLED);
        supportRequestRepository.save(supportRequest);
    }
}
