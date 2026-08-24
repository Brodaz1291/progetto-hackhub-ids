package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.InvitationDTO;
import it.unicam.cs.hackhub.application.dtos.TeamDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.InvitationService;
import it.unicam.cs.hackhub.application.services.TeamService;
import it.unicam.cs.hackhub.controllers.requests.CreateTeamRequest;
import it.unicam.cs.hackhub.designPatterns.facade.TeamFacade;
import it.unicam.cs.hackhub.model.entities.Invitation;
import it.unicam.cs.hackhub.model.entities.Team;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final InvitationService invitationService;
    private final TeamFacade teamFacade;
    private final DTOMapper dtoMapper;

    public TeamController(TeamService teamService,
                          InvitationService invitationService,
                          TeamFacade teamFacade,
                          DTOMapper dtoMapper) {
        this.teamService = teamService;
        this.invitationService = invitationService;
        this.teamFacade = teamFacade;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Creates a team on behalf of the user who becomes its first member.
     *
     * NOTE: the identity of the creator travels as an explicit parameter, as the model
     * prescribes, instead of being read from the security context.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDTO createTeam(@RequestBody CreateTeamRequest req,
                              @RequestParam Long creatorId) {
        Team createdTeam = teamService.createTeam(req, creatorId);
        return dtoMapper.toDTO(createdTeam);
    }

    /**
     * Removes the membership of a user: the path carries no team id because the membership is
     * unique, so the user alone identifies it.
     */
    @DeleteMapping("/members")
    public void leaveTeam(@RequestParam Long userId) {
        teamService.leaveTeam(userId);
    }

    @PostMapping("/{teamId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationDTO sendInvitation(@PathVariable Long teamId,
                                        @RequestParam String username,
                                        @RequestParam Long senderId) {
        Invitation invitation = teamFacade.sendInvitation(teamId, username, senderId);
        return dtoMapper.toDTO(invitation);
    }

    /**
     * Returns the invitations a user has received and still has to answer: the list belongs
     * to the invited user, not to a team, so the path carries no team id.
     */
    @GetMapping("/invitations")
    public List<InvitationDTO> getInvitations(@RequestParam Long userId) {
        List<Invitation> invitations = teamFacade.getInvitations(userId);
        return invitations.stream().map(dtoMapper::toDTO).toList();
    }

    @PutMapping("/invitations/{invitationId}")
    public InvitationDTO acceptInvitation(@PathVariable Long invitationId,
                                          @RequestParam Long userId) {
        Invitation acceptedInvitation = teamFacade.acceptInvitation(invitationId, userId);
        return dtoMapper.toDTO(acceptedInvitation);
    }

    /**
     * Refuses an invitation. It does not go through the facade because no other service is
     * involved: the refusal creates no membership and notifies nobody.
     *
     * The path carries a sub-resource because the acceptance already answers PUT on the
     * invitation itself, and the two answers cannot share verb and path.
     */
    @PutMapping("/invitations/{invitationId}/refusal")
    public InvitationDTO declineInvitation(@PathVariable Long invitationId,
                                           @RequestParam Long userId) {
        Invitation declinedInvitation = invitationService.declineInvitation(invitationId, userId);
        return dtoMapper.toDTO(declinedInvitation);
    }
}
