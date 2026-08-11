package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.InvitationDTO;
import it.unicam.cs.hackhub.application.dtos.TeamDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.TeamService;
import it.unicam.cs.hackhub.controllers.requests.CreateTeamRequest;
import it.unicam.cs.hackhub.designPatterns.TeamFacade;
import it.unicam.cs.hackhub.model.entities.Invitation;
import it.unicam.cs.hackhub.model.entities.Team;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final TeamFacade teamFacade;
    private final DTOMapper dtoMapper;

    public TeamController(TeamService teamService, TeamFacade teamFacade, DTOMapper dtoMapper) {
        this.teamService = teamService;
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
    public TeamDTO createTeam(@RequestBody CreateTeamRequest req,
                              @RequestParam Long creatorId) {
        Team createdTeam = teamService.createTeam(req, creatorId);
        return dtoMapper.toDTO(createdTeam);
    }

    @PostMapping("/{teamId}/invitations")
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
}
