package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    /**
     * Active participations of a user: every TeamMember (a closed membership is
     * deleted, not kept), plus StaffParticipations whose hackathon is not CONCLUDED.
     */
    @Query("""
            SELECT p FROM Participation p
            WHERE p.user.id = :userId
              AND (TYPE(p) = TeamMember
                   OR p.id IN (SELECT sp.id FROM StaffParticipation sp
                               WHERE sp.hackathon.state <> it.unicam.cs.hackhub.model.enums.HackathonState.CONCLUDED))
            """)
    List<Participation> findActiveByUserId(@Param("userId") Long userId);
}
