package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.SubmissionDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
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
    private final DTOMapper dtoMapper;

    public SubmissionController(SubmissionService submissionService, DTOMapper dtoMapper) {
        this.submissionService = submissionService;
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

    @GetMapping
    public List<SubmissionDTO> getSubmissions(@RequestParam Long hackathonId) {
        return submissionService.getSubmissions(hackathonId).stream()
                .map(dtoMapper::toDTO)
                .toList();
    }

    @GetMapping("/{submissionId}")
    public SubmissionDTO getSubmission(@PathVariable Long submissionId) {
        return dtoMapper.toDTO(submissionService.getSubmission(submissionId));
    }
}
