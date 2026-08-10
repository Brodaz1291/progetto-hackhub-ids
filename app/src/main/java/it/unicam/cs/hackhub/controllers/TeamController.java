package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.TeamDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.TeamService;
import it.unicam.cs.hackhub.controllers.requests.CreateTeamRequest;
import it.unicam.cs.hackhub.model.entities.Team;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final DTOMapper dtoMapper;

    public TeamController(TeamService teamService, DTOMapper dtoMapper) {
        this.teamService = teamService;
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
}
