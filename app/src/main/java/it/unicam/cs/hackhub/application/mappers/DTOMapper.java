package it.unicam.cs.hackhub.application.mappers;

import it.unicam.cs.hackhub.application.dtos.*;
import it.unicam.cs.hackhub.model.entities.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DTOMapper {

    public UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getUsername());
    }

    public TeamDTO toDTO(Team team) {
        List<String> memberUsernames = team.getMembers().stream()
                .map(member -> member.getUser().getUsername())
                .toList();
        return new TeamDTO(team.getId(), team.getName(), memberUsernames);
    }

    public HackathonDTO toDTO(Hackathon hackathon) {
        String winnerTeamName = hackathon.getWinner() != null
                ? hackathon.getWinner().getTeam().getName()
                : null;
        return new HackathonDTO(
                hackathon.getId(),
                hackathon.getName(),
                hackathon.getLocation(),
                hackathon.getPrize(),
                hackathon.getRules(),
                hackathon.getRegistrationDeadline(),
                hackathon.getStartDate(),
                hackathon.getEndDate(),
                hackathon.getMaxTeamSize(),
                hackathon.getState(),
                winnerTeamName);
    }

    public SubmissionDTO toDTO(Submission submission) {
        Integer score = null;
        String judgment = null;
        if (submission.getEvaluation() != null) {
            score = submission.getEvaluation().getScore();
            judgment = submission.getEvaluation().getJudgment();
        }
        return new SubmissionDTO(
                submission.getId(),
                submission.getTitle(),
                submission.getDescription(),
                submission.getLink(),
                submission.getSubmissionDate(),
                submission.getRegistration().getTeam().getName(),
                score,
                judgment);
    }

    public InvitationDTO toDTO(Invitation invitation) {
        return new InvitationDTO(
                invitation.getId(),
                invitation.getTeam().getName(),
                invitation.getSender().getUser().getUsername(),
                invitation.getStatus());
    }

    public SupportRequestDTO toDTO(SupportRequest supportRequest) {
        LocalDateTime callDateTime = supportRequest.getCall() != null
                ? supportRequest.getCall().getDateTime()
                : null;
        return new SupportRequestDTO(
                supportRequest.getId(),
                supportRequest.getDescription(),
                supportRequest.getStatus(),
                supportRequest.getRegistration().getTeam().getName(),
                callDateTime);
    }

    public ReportDTO toDTO(Report report) {
        return new ReportDTO(
                report.getId(),
                report.getReason(),
                report.getDate(),
                report.getRegistration().getTeam().getName());
    }

    public PaymentDTO toDTO(Payment payment) {
        return new PaymentDTO(payment.getId(), payment.getAmount(), payment.getDate());
    }

    public HackathonDTO toPublicDTO(Hackathon hackathon) {
        String winnerTeamName = hackathon.getWinner() != null
                ? hackathon.getWinner().getTeam().getName()
                : null;
        return new HackathonDTO(
                null,
                hackathon.getName(),
                hackathon.getLocation(),
                hackathon.getPrize(),
                hackathon.getRules(),
                hackathon.getRegistrationDeadline(),
                hackathon.getStartDate(),
                hackathon.getEndDate(),
                hackathon.getMaxTeamSize(),
                hackathon.getState(),
                winnerTeamName);
    }
}
