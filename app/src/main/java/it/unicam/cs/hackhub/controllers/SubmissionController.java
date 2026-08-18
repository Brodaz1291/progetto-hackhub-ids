package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.SubmissionDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.EvaluationService;
import it.unicam.cs.hackhub.application.services.SubmissionService;
import it.unicam.cs.hackhub.model.entities.Submission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final EvaluationService evaluationService;
    private final DTOMapper dtoMapper;

    public SubmissionController(SubmissionService submissionService,
                                EvaluationService evaluationService,
                                DTOMapper dtoMapper) {
        this.submissionService = submissionService;
        this.evaluationService = evaluationService;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Uploads the submission of a team, replacing the previous one if it already exists.
     *
     * NOTE: the projection is returned instead of nothing because the client needs the
     * identifier of the submission to read it back afterwards.
     */
    @PostMapping
    public SubmissionDTO uploadSubmission(@RequestParam Long registrationId,
                                          @RequestParam String title,
                                          @RequestParam String description,
                                          @RequestParam String link) {
        Submission submission = submissionService.uploadSubmission(registrationId, title, description, link);
        return dtoMapper.toDTO(submission);
    }

    /**
     * The identity of who is reading travels as a parameter because the access to the
     * elaborates is granted to the staff of that hackathon only.
     */
    @GetMapping
    public List<SubmissionDTO> getSubmissions(@RequestParam Long hackathonId,
                                              @RequestParam Long userId) {
        return submissionService.getSubmissions(hackathonId, userId).stream()
                .map(dtoMapper::toDTO)
                .toList();
    }

    @GetMapping("/{submissionId}")
    public SubmissionDTO getSubmission(@PathVariable Long submissionId) {
        return dtoMapper.toDTO(submissionService.getSubmission(submissionId));
    }

    /**
     * Records the score and the judgment a judge expresses on a submission.
     *
     * NOTE: the submission is returned instead of the evaluation because its projection
     * already carries score and judgment, so the client reads the elaborate back with the
     * verdict attached.
     *
     * It is read again because the association goes from the submission to its evaluation
     * only, and that is on purpose: an evaluation makes no sense detached from the elaborate
     * it judges, which is the one owning it. Nobody starts from an evaluation looking for its
     * submission, so the second reading is the price of keeping the association pointing the
     * only way it is travelled.
     */
    @PostMapping("/{submissionId}/evaluation")
    public SubmissionDTO evaluateSubmission(@PathVariable Long submissionId,
                                            @RequestParam int score,
                                            @RequestParam String judgment) {
        evaluationService.evaluateSubmission(submissionId, score, judgment);
        Submission evaluated = submissionService.getSubmission(submissionId);
        return dtoMapper.toDTO(evaluated);
    }
}
