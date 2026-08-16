package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.model.entities.Evaluation;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Submission;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.enums.RegistrationState;
import it.unicam.cs.hackhub.model.repositories.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 10;

    private final SubmissionRepository submissionRepository;
    private final HackathonLifecycle hackathonLifecycle;

    public EvaluationService(SubmissionRepository submissionRepository,
                             HackathonLifecycle hackathonLifecycle) {
        this.submissionRepository = submissionRepository;
        this.hackathonLifecycle = hackathonLifecycle;
    }

    /**
     * Records the evaluation of a submission. Evaluating again replaces the previous score and
     * judgment: the judge can change his mind until the hackathon is over, exactly as the team
     * can hand in its elaborate again while the event is running.
     */
    @Transactional
    public Evaluation evaluateSubmission(Long submissionId, int score, String judgment) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        Registration registration = submission.getRegistration();
        hackathonLifecycle.refreshState(registration.getHackathon());

        if (!checkHackathonInEvaluation(registration.getHackathon())) {
            throw new IllegalArgumentException("Submissions can be evaluated only while the hackathon is in evaluation");
        }
        if (!checkNotDisqualified(registration)) {
            throw new IllegalArgumentException("The submission of a disqualified team cannot be evaluated");
        }
        if (!checkScore(score)) {
            throw new IllegalArgumentException("The score must be between " + MIN_SCORE + " and " + MAX_SCORE);
        }

        Evaluation evaluation = submission.getEvaluation();
        if (evaluation == null) {
            evaluation = new Evaluation(score, judgment);
            submission.setEvaluation(evaluation);
        } else {
            evaluation.setScore(score);
            evaluation.setJudgment(judgment);
        }
        // the evaluation has no repository of its own: it is saved along with the submission
        // that owns it
        submissionRepository.save(submission);
        return evaluation;
    }

    /**
     * The judgment belongs to the evaluation phase: while the event is running the team can
     * still upload its elaborate again, so a score expressed then would fall on a content that
     * can still change.
     */
    private boolean checkHackathonInEvaluation(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.EVALUATION;
    }

    /**
     * The submissions of the disqualified teams are already left out of the list the judge
     * reads, so they are checked here too: reaching one means having passed its identifier by
     * hand.
     */
    private boolean checkNotDisqualified(Registration registration) {
        return registration.getState() != RegistrationState.DISQUALIFIED;
    }

    private boolean checkScore(int score) {
        return score >= MIN_SCORE && score <= MAX_SCORE;
    }
}
