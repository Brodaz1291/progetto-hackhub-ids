package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.ConflictException;
import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.model.entities.Invitation;
import it.unicam.cs.hackhub.model.entities.Participation;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.entities.TeamMember;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.enums.InvitationState;
import it.unicam.cs.hackhub.model.repositories.InvitationRepository;
import it.unicam.cs.hackhub.model.repositories.ParticipationRepository;
import it.unicam.cs.hackhub.model.repositories.TeamRepository;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ParticipationRepository participationRepository;
    private final TeamService teamService;

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             TeamRepository teamRepository,
                             ParticipationRepository participationRepository,
                             TeamService teamService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.participationRepository = participationRepository;
        this.teamService = teamService;
    }

    /**
     * Registers a pending invitation from a team member to another user of the platform.
     * The invited user is notified by the facade, which coordinates this service with the
     * notification one.
     */
    public Invitation sendInvitation(Long teamId, String username, Long senderId) {
        User invitedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        if (!checkNoActiveParticipation(invitedUser)) {
            throw new ConflictException("User already takes part in something else: " + username);
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found: " + teamId));
        // the membership belongs to TeamService: the sender is resolved through it
        TeamMember sender = teamService.getTeamMember(senderId);

        Invitation invitation = new Invitation(sender, team, invitedUser, InvitationState.PENDING);
        return invitationRepository.save(invitation);
    }

    /**
     * Returns the invitations a user still has to answer.
     */
    public List<Invitation> getInvitations(Long userId) {
        return invitationRepository.findPendingByUser(userId);
    }

    /**
     * Marks an invitation as accepted. The membership is not created here: it is
     * TeamService that creates it, called by the facade right after this method.
     */
    public Invitation acceptInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        // an invitation is answered by its recipient only, otherwise knowing an id would be
        // enough to join a team one was never invited to
        Long recipientId = invitation.getRecipient().getId();
        if (!recipientId.equals(userId)) {
            throw new ValidationException("Invitation " + invitationId + " is not addressed to user " + userId);
        }
        if (invitation.getStatus() != InvitationState.PENDING) {
            throw new ConflictException("Invitation " + invitationId + " has already been answered");
        }
        // the user may have joined a team or become staff between the invitation and its
        // acceptance, so the check made on sending is not enough
        if (!checkNoActiveParticipation(user)) {
            throw new ConflictException("User already takes part in something else: " + userId);
        }

        markAccepted(invitation);
        return invitationRepository.save(invitation);
    }

    /**
     * Marks an invitation as refused. Unlike the acceptance no membership follows, so the
     * invitation simply stops being pending and no other aggregate is touched.
     */
    public Invitation declineInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        // as for the acceptance, only the recipient answers an invitation
        Long recipientId = invitation.getRecipient().getId();
        if (!recipientId.equals(user.getId())) {
            throw new ValidationException("Invitation " + invitationId + " is not addressed to user " + userId);
        }
        if (invitation.getStatus() != InvitationState.PENDING) {
            throw new ConflictException("Invitation " + invitationId + " has already been answered");
        }
        // no participation check here: refusing creates no membership, so a user who joined a
        // team in the meantime must still be able to close the invitations left pending

        markDeclined(invitation);
        return invitationRepository.save(invitation);
    }

    /**
     * A user takes part in one thing at a time: either a team membership or a staff role in
     * a hackathon that is still running. The query looks at both branches of the hierarchy.
     */
    private boolean checkNoActiveParticipation(User user) {
        List<Participation> activeParticipations = participationRepository.findActiveByUserId(user.getId());
        return activeParticipations.isEmpty();
    }

    private void markAccepted(Invitation invitation) {
        invitation.setStatus(InvitationState.ACCEPTED);
    }

    private void markDeclined(Invitation invitation) {
        invitation.setStatus(InvitationState.DECLINED);
    }
}
