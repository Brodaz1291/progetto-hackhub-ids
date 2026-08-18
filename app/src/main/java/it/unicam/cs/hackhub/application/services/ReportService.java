package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Organizer;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Report;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.repositories.RegistrationRepository;
import it.unicam.cs.hackhub.model.repositories.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final HackathonLifecycle hackathonLifecycle;
    private final Clock clock;

    public ReportService(ReportRepository reportRepository,
                         RegistrationRepository registrationRepository,
                         NotificationService notificationService,
                         HackathonLifecycle hackathonLifecycle,
                         Clock clock) {
        this.reportRepository = reportRepository;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
        this.hackathonLifecycle = hackathonLifecycle;
        this.clock = clock;
    }

    /**
     * Records the report of a team and notifies the organizer of the hackathon.
     *
     * The report does not keep track of who sent it: it is a fact about the team, and the
     * organizer decides on the merit of the violation, not on who noticed it.
     */
    @Transactional
    public Report reportTeam(Long registrationId, String reason) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found: " + registrationId));
        Hackathon hackathon = registration.getHackathon();
        hackathonLifecycle.refreshState(hackathon);
        if (!checkHackathonIsRunning(hackathon)) {
            throw new ValidationException("A team can be reported only while the hackathon is running");
        }
        if (!checkReason(reason)) {
            throw new ValidationException("Reason of the report is required");
        }

        Report report = new Report(registration, reason, LocalDateTime.now(clock));
        Report savedReport = reportRepository.save(report);

        Organizer organizer = hackathon.getStaff().stream()
                .filter(Organizer.class::isInstance)
                .map(Organizer.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hackathon " + hackathon.getId() + " has no organizer"));
        notificationService.notifyOrganizer(organizer);

        return savedReport;
    }

    /**
     * A report is about behaviour held during the event: before it starts there is nothing to
     * report yet, and once the submissions are closed the conduct of the teams is settled.
     */
    private boolean checkHackathonIsRunning(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.RUNNING;
    }

    private boolean checkReason(String reason) {
        return StringUtils.hasText(reason);
    }

    /**
     * Returns the reports collected by a hackathon, for the organizer to consult.
     */
    public List<Report> getReports(Long hackathonId) {
        return reportRepository.findByHackathonId(hackathonId);
    }

    public Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + reportId));
    }
}
