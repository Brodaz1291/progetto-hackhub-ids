package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.ConflictException;
import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.controllers.requests.CreateTeamRequest;
import it.unicam.cs.hackhub.model.entities.Participation;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.entities.TeamMember;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.repositories.ParticipationRepository;
import it.unicam.cs.hackhub.model.repositories.RegistrationRepository;
import it.unicam.cs.hackhub.model.repositories.TeamMemberRepository;
import it.unicam.cs.hackhub.model.repositories.TeamRepository;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final HackathonLifecycle hackathonLifecycle;

    public TeamService(TeamRepository teamRepository,
                       UserRepository userRepository,
                       ParticipationRepository participationRepository,
                       TeamMemberRepository teamMemberRepository,
                       RegistrationRepository registrationRepository,
                       HackathonLifecycle hackathonLifecycle) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.registrationRepository = registrationRepository;
        this.hackathonLifecycle = hackathonLifecycle;
    }

    /**
     * Creates a team and registers the user who asked for it as its first member.
     */
    public Team createTeam(CreateTeamRequest req, Long creatorId) {
        String name = req.getName();
        if (!checkName(name)) {
            throw new ValidationException("Invalid or already used team name: " + name);
        }
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("Creator not found: " + creatorId));
        if (!checkNoActiveParticipation(creator)) {
            throw new ConflictException("User already takes part in something else: " + creatorId);
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

    /**
     * Returns the membership of a user. A user belongs to at most one team at a time, so the
     * membership is unique when it exists.
     */
    public TeamMember getTeamMember(Long userId) {
        return teamMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User is not a member of any team: " + userId));
    }

    /**
     * Removes the membership of a user from the team they belong to. When the one who leaves
     * is the last member, the team is kept only if it has a history to preserve, otherwise
     * it is dissolved.
     */
    @Transactional
    public void leaveTeam(Long userId) {
        TeamMember leavingMember = teamMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User is not a member of any team: " + userId));
        Team team = leavingMember.getTeam();
        // the phases are brought up to date before any check reads them: the transitions are
        // applied on reading, so a hackathon whose start date has passed still looks open
        // until somebody reads it, and the team could be left in the middle of the event
        List<Registration> registrations = registrationRepository.findByTeamId(team.getId());
        registrations.forEach(registration -> hackathonLifecycle.refreshState(registration.getHackathon()));

        if (!checkNoOngoingParticipation(team)) {
            throw new ValidationException(
                    "A team cannot be left while it takes part in a hackathon in progress: " + team.getId());
        }

        // removing the membership from the list is what deletes it: orphanRemoval takes care
        // of the row and, in cascade, of the invitations that member had sent
        team.getMembers().removeIf(member -> member.getId().equals(leavingMember.getId()));

        if (team.getMembers().isEmpty()) {
            // a team without members cannot compete: the registrations still open are withdrawn
            // whatever happens to the team. They belong to the hackathon rather than to the team,
            // so no cascade reaches them and they go explicitly
            List<Registration> openRegistrations = registrations.stream()
                    .filter(registration -> registration.getHackathon().getState() == HackathonState.REGISTRATION)
                    .toList();
            registrationRepository.deleteAll(openRegistrations);

            if (checkHasConcludedParticipation(team)) {
                // the team stays as a historical shell: a concluded hackathon must keep its
                // winner, and the past submissions stay attached to the team that wrote them
                teamRepository.save(team);
            } else {
                teamRepository.delete(team);
            }
        } else {
            teamRepository.save(team);
        }
    }

    /**
     * A team can be left while it is still forming, not once the competition has started: a
     * member walking out halfway would alter the standings.
     */
    private boolean checkNoOngoingParticipation(Team team) {
        List<Registration> registrations = registrationRepository.findByTeamId(team.getId());
        for (Registration registration : registrations) {
            HackathonState hackathonState = registration.getHackathon().getState();
            if (hackathonState == HackathonState.RUNNING || hackathonState == HackathonState.EVALUATION) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the team took part in at least one hackathon that has been concluded,
     * which is the history that keeps an empty team alive.
     */
    private boolean checkHasConcludedParticipation(Team team) {
        List<Registration> registrations = registrationRepository.findByTeamId(team.getId());
        for (Registration registration : registrations) {
            if (registration.getHackathon().getState() == HackathonState.CONCLUDED) {
                return true;
            }
        }
        return false;
    }
}
