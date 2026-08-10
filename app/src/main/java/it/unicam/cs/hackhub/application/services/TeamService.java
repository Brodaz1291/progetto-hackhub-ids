package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.controllers.requests.CreateTeamRequest;
import it.unicam.cs.hackhub.model.entities.Participation;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.entities.TeamMember;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.repositories.ParticipationRepository;
import it.unicam.cs.hackhub.model.repositories.TeamRepository;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;

    public TeamService(TeamRepository teamRepository,
                       UserRepository userRepository,
                       ParticipationRepository participationRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }

    /**
     * Creates a team and registers the user who asked for it as its first member.
     */
    public Team createTeam(CreateTeamRequest req, Long creatorId) {
        String name = req.getName();
        if (!checkName(name)) {
            throw new IllegalArgumentException("Invalid or already used team name: " + name);
        }
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found: " + creatorId));
        if (!checkNoActiveParticipation(creator)) {
            throw new IllegalArgumentException("User already takes part in something else: " + creatorId);
        }

        Team newTeam = new Team(name, req.getIban());
        addMember(creator, newTeam);
        return teamRepository.save(newTeam);
    }

    private boolean checkName(String name) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return !teamRepository.existsByName(name);
    }

    /**
     * A user takes part in one thing at a time: either a team membership or a staff role in
     * a hackathon that is still running. The query looks at both branches of the hierarchy.
     */
    private boolean checkNoActiveParticipation(User user) {
        List<Participation> activeParticipations = participationRepository.findActiveByUserId(user.getId());
        return activeParticipations.isEmpty();
    }

    /**
     * Adds a user to a team as a new member.
     *
     * The new member points to its team and appears in the list of the team as well: Team
     * cascades the persistence to its members, so a member left out of the list would never
     * be saved.
     */
    public void addMember(User user, Team team) {
        TeamMember newMember = new TeamMember(user, team);
        team.getMembers().add(newMember);
    }
}
