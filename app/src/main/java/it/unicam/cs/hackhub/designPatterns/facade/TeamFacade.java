package it.unicam.cs.hackhub.designPatterns.facade;

import it.unicam.cs.hackhub.application.services.InvitationService;
import it.unicam.cs.hackhub.application.services.NotificationService;
import it.unicam.cs.hackhub.application.services.TeamService;
import it.unicam.cs.hackhub.model.entities.Invitation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Single entry point for the invitation flow, which is the one case where more than one
 * service takes part: InvitationService owns the invitation, TeamService owns the
 * membership and NotificationService warns the invited user. None of them knows the
 * others: the order in which they are called lives here.
 */
@Component
public class TeamFacade {

    private final InvitationService invitationService;
    private final TeamService teamService;
    private final NotificationService notificationService;

    public TeamFacade(InvitationService invitationService,
                      TeamService teamService,
                      NotificationService notificationService) {
        this.invitationService = invitationService;
        this.teamService = teamService;
        this.notificationService = notificationService;
    }

    public Invitation sendInvitation(Long teamId, String username, Long senderId) {
        Invitation invitation = invitationService.sendInvitation(teamId, username, senderId);
        notificationService.notifyUser(invitation.getRecipient());
        return invitation;
    }

    public List<Invitation> getInvitations(Long userId) {
        return invitationService.getInvitations(userId);
    }

    /**
     * Accepting an invitation touches two different aggregates: the state of the invitation
     * and the membership. If the membership failed after the invitation has been marked as
     * accepted, the user would end up with an accepted invitation but outside of the team.
     * The transaction makes the two steps one.
     */
    @Transactional
    public Invitation acceptInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationService.acceptInvitation(invitationId, userId);
        teamService.addMember(invitation.getRecipient(), invitation.getTeam());
        return invitation;
    }
}
