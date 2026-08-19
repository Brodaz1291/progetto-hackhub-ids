package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("""
            SELECT s FROM Submission s
            WHERE s.registration.hackathon.id = :hackathonId
              AND s.registration.state <> it.unicam.cs.hackhub.model.enums.RegistrationState.DISQUALIFIED
            """)
    List<Submission> findByHackathonIdNotDisqualified(@Param("hackathonId") Long hackathonId);

    @Query("""
            SELECT s FROM Submission s
            WHERE s.registration.hackathon.id = :hackathonId
              AND s.registration.state <> it.unicam.cs.hackhub.model.enums.RegistrationState.DISQUALIFIED
              AND s.evaluation IS NOT NULL
            """)
    List<Submission> findEvaluatedByHackathonId(@Param("hackathonId") Long hackathonId);
}
