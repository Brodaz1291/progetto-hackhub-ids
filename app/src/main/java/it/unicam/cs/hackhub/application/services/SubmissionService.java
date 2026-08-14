package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Submission;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.enums.RegistrationState;
import it.unicam.cs.hackhub.model.repositories.RegistrationRepository;
import it.unicam.cs.hackhub.model.repositories.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final RegistrationRepository registrationRepository;

    public SubmissionService(SubmissionRepository submissionRepository,
                             RegistrationRepository registrationRepository) {
        this.submissionRepository = submissionRepository;
        this.registrationRepository = registrationRepository;
    }

    /**
     * Uploads the submission of a team. Uploading and updating are the same operation: until
     * the event is over the team can submit again, and the new data replaces the old one.
     */
    @Transactional
    public Submission uploadSubmission(Long registrationId, String title, String description, String link) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));
        if (!checkHackathonIsRunning(registration.getHackathon())) {
            throw new IllegalArgumentException("Submissions are accepted only while the hackathon is running");
        }
        if (!checkNotDisqualified(registration)) {
            throw new IllegalArgumentException("Registration " + registrationId + " is disqualified");
        }
        if (!checkSubmissionData(title, description, link)) {
            throw new IllegalArgumentException("Title, description and link of the submission are required");
        }

        Submission submission = registration.getSubmission();
        if (submission == null) {
            submission = new Submission(registration, title, description, link, LocalDateTime.now());
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
        submission.setSubmissionDate(LocalDateTime.now());
    }
}
