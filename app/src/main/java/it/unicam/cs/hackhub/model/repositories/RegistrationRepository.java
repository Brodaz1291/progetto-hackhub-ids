package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    @Query("""
            SELECT r FROM Registration r
            WHERE r.team = :team
              AND r.hackathon.state <> it.unicam.cs.hackhub.model.enums.HackathonState.CONCLUDED
            """)
    Optional<Registration> findActiveByTeam(@Param("team") Team team);

    List<Registration> findByTeamId(Long teamId);

    List<Registration> findByHackathonId(Long hackathonId);

    @Query("SELECT r FROM Hackathon h JOIN h.winner r WHERE h.id = :hackathonId")
    Optional<Registration> findWinnerByHackathonId(@Param("hackathonId") Long hackathonId);
}
