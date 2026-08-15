package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.ReportDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.ReportService;
import it.unicam.cs.hackhub.model.entities.Report;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final DTOMapper dtoMapper;

    public ReportController(ReportService reportService, DTOMapper dtoMapper) {
        this.reportService = reportService;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Reports to the organizer a team that violated the rules of the hackathon.
     */
    @PostMapping
    public ReportDTO reportTeam(@RequestParam Long registrationId,
                                @RequestParam String reason) {
        Report report = reportService.reportTeam(registrationId, reason);
        return dtoMapper.toDTO(report);
    }

    /**
     * Returns the reports collected by a hackathon, for the organizer to consult.
     */
    @GetMapping
    public List<ReportDTO> getReports(@RequestParam Long hackathonId) {
        List<Report> reports = reportService.getReports(hackathonId);
        return reports.stream()
                .map(dtoMapper::toDTO)
                .toList();
    }

    @GetMapping("/{reportId}")
    public ReportDTO getReport(@PathVariable Long reportId) {
        return dtoMapper.toDTO(reportService.getReport(reportId));
    }
}
