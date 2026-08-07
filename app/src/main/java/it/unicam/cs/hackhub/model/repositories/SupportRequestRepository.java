package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    @Query("""
            SELECT sr FROM SupportRequest sr
            WHERE sr.registration.hackathon.id = :hackathonId
              AND sr.status = it.unicam.cs.hackhub.model.enums.SupportRequestState.PENDING
            """)
    List<SupportRequest> findPendingByHackathonId(@Param("hackathonId") Long hackathonId);

    List<SupportRequest> findByRegistrationId(Long registrationId);
}
