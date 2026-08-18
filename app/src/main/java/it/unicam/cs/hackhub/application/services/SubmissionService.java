package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.StaffParticipation;
import it.unicam.cs.hackhub.model.entities.Submission;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.enums.RegistrationState;
import it.unicam.cs.hackhub.model.repositories.HackathonRepository;
import it.unicam.cs.hackhub.model.repositories.RegistrationRepository;
import it.unicam.cs.hackhub.model.repositories.SubmissionRepository;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final RegistrationRepository registrationRepository;
    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final HackathonLifecycle hackathonLifecycle;
    private final Clock clock;

    public SubmissionService(SubmissionRepository submissionRepository,
                             RegistrationRepository registrationRepository,
                             HackathonRepository hackathonRepository,
                             UserRepository userRepository,
                             HackathonLifecycle hackathonLifecycle,
                             Clock clock) {
        this.submissionRepository = submissionRepository;
        this.registrationRepository = registrationRepository;
        this.hackathonRepository = hackathonRepository;
        this.userRepository = userRepository;
        this.hackathonLifecycle = hackathonLifecycle;
        this.clock = clock;
    }

    /**
     * Uploads the submission of a team. Uploading and updating are the same operation: until
     * the event is over the team can submit again, and the new data replaces the old one.
     */
    @Transactional
    public Submission uploadSubmission(Long registrationId, String title, String description, String link) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found: " + registrationId));
        hackathonLifecycle.refreshState(registration.getHackathon());
        if (!checkHackathonIsRunning(registration.getHackathon())) {
            throw new ValidationException("Submissions are accepted only while the hackathon is running");
        }
        if (!checkNotDisqualified(registration)) {
            throw new ValidationException("Registration " + registrationId + " is disqualified");
        }
        if (!checkSubmissionData(title, description, link)) {
            throw new ValidationException("Title, description and link of the submission are required");
        }

        Submission submission = registration.getSubmission();
        if (submission == null) {
            submission = new Submission(registration, title, description, link, LocalDateTime.now(clock));
            // the registration is managed inside the transaction, so the submission is linked
            // on both sides to keep the in-memory graph coherent for whoever reads it next
            registration.setSubmission(submission);
        } else {
            replaceSubmissionData(submission, title, description, link);
        }
        return submissionRepository.save(submission);
    }

    /**
     * Submissions belong to the event in progress: before it starts there is nothing to hand
     * in yet, once it is over the elaborates are frozen for the evaluation.
     */
    private boolean checkHackathonIsRunning(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.RUNNING;
    }

    private boolean checkNotDisqualified(Registration registration) {
        return registration.getState() != RegistrationState.DISQUALIFIED;
    }

    private boolean checkSubmissionData(String title, String description, String link) {
        return StringUtils.hasText(title) && StringUtils.hasText(description) && StringUtils.hasText(link);
    }

    /**
     * The overwriting updates the existing submission instead of creating another one: the
     * identity of the elaborate is the same, so whatever is already attached to it survives
     * the update.
     */
    private void replaceSubmissionData(Submission submission, String title, String description, String link) {
        submission.setTitle(title);
        submission.setDescription(description);
        submission.setLink(link);
        submission.setSubmissionDate(LocalDateTime.now(clock));
    }

    /**
     * Returns the submissions of a hackathon, leaving out the ones of the disqualified teams:
     * they are excluded by projection, so they never reach who reads this list.
     *
     * The reading is reserved to the staff of that same hackathon, and it is the only one of
     * the system that depends on who is asking for it.
     */
    public List<Submission> getSubmissions(Long hackathonId, Long userId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (!checkStaffAssigned(user, hackathon)) {
            throw new ValidationException(
                    "User " + userId + " is not staff of hackathon " + hackathonId);
        }
        return submissionRepository.findByHackathonIdNotDisqualified(hackathonId);
    }

    /**
     * The role covered makes no difference: organizer, judge and mentor all consult the
     * elaborates of their own event. What is decisive is the membership, so it is enough to
     * find the user among the staff participations of this hackathon.
     *
     * The staff is read outside a transaction because HackathonRepository.findById fetches it
     * together with the hackathon: removing that join fetch breaks this check.
     */
    private boolean checkStaffAssigned(User user, Hackathon hackathon) {
        for (StaffParticipation staffParticipation : hackathon.getStaff()) {
            if (staffParticipation.getUser().getId().equals(user.getId())) {
                return true;
            }
        }
        return false;
    }

    public Submission getSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));
    }
}
