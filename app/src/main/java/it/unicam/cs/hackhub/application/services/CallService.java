package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.designPatterns.CallScheduler;
import it.unicam.cs.hackhub.model.entities.Call;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Mentor;
import it.unicam.cs.hackhub.model.entities.SupportRequest;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.enums.SupportRequestState;
import it.unicam.cs.hackhub.model.repositories.CallRepository;
import it.unicam.cs.hackhub.model.repositories.SupportRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CallService {

    private final CallRepository callRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final SupportRequestService supportRequestService;
    private final CallScheduler callScheduler;
    private final NotificationService notificationService;
    private final HackathonLifecycle hackathonLifecycle;
    private final Clock clock;

    public CallService(CallRepository callRepository,
                       SupportRequestRepository supportRequestRepository,
                       SupportRequestService supportRequestService,
                       CallScheduler callScheduler,
                       NotificationService notificationService,
                       HackathonLifecycle hackathonLifecycle,
                       Clock clock) {
        this.callRepository = callRepository;
        this.supportRequestRepository = supportRequestRepository;
        this.supportRequestService = supportRequestService;
        this.callScheduler = callScheduler;
        this.notificationService = notificationService;
        this.hackathonLifecycle = hackathonLifecycle;
        this.clock = clock;
    }

    /**
     * Books the slot on the external calendar and records the call the support request asked
     * for, which is what takes the request over.
     *
     * The booking comes before the call is recorded: if the calendar refuses the slot there
     * is nothing to record, and a call the mentor and the team have no appointment for would
     * be worse than no call at all. The transaction covers the two writes that follow,
     * because a request marked as handled without its call would leave the team waiting for
     * an appointment nobody planned.
     */
    @Transactional
    public Call scheduleCall(Long supportRequestId, Long mentorId, LocalDateTime dateTime) {
        SupportRequest supportRequest = supportRequestRepository.findById(supportRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Support request not found: " + supportRequestId));
        Hackathon hackathon = supportRequest.getRegistration().getHackathon();
        hackathonLifecycle.refreshState(hackathon);
        Mentor mentor = findMentor(hackathon, mentorId);
        if (!checkHackathonIsRunning(hackathon)) {
            throw new IllegalArgumentException("A call can be scheduled only while the hackathon is running");
        }
        if (!checkSlot(dateTime)) {
            throw new IllegalArgumentException("The call must be planned on a future instant: " + dateTime);
        }
        if (!checkRequestIsPending(supportRequest)) {
            throw new IllegalArgumentException(
                    "Support request " + supportRequestId + " has already been taken over by a mentor");
        }

        Team team = supportRequest.getRegistration().getTeam();
        String externalId = callScheduler.bookSlot(mentor, team, dateTime);
        Call call = callRepository.save(new Call(supportRequest, mentor, dateTime, externalId));
        // the request owns no column of the call, so the link back is not filled in on its
        // own: without this the response would describe a request with no appointment on it
        supportRequest.setCall(call);
        supportRequestService.markHandled(supportRequest);
        notificationService.notifyTeam(team);

        return call;
    }

    /**
     * The mentor is looked for inside the staff of the hackathon the request comes from:
     * finding it there makes both the role and the membership implicit, so a mentor of
     * another event cannot answer a request that is not addressed to them.
     */
    private Mentor findMentor(Hackathon hackathon, Long mentorId) {
        return hackathon.getStaff().stream()
                .filter(Mentor.class::isInstance)
                .map(Mentor.class::cast)
                .filter(mentor -> mentor.getUser().getId().equals(mentorId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + mentorId + " is not a mentor of hackathon " + hackathon.getId()));
    }

    /**
     * A call supports a team while it works: before the event starts there is nothing to be
     * helped with, and once the submissions are closed the work is over.
     */
    private boolean checkHackathonIsRunning(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.RUNNING;
    }

    private boolean checkSlot(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(LocalDateTime.now(clock));
    }

    /**
     * A request is answered once: a second call on the same request would leave it with two
     * appointments, where the model gives it at most one.
     */
    private boolean checkRequestIsPending(SupportRequest supportRequest) {
        return supportRequest.getStatus() == SupportRequestState.PENDING;
    }
}
