package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @Query("""
            SELECT i FROM Invitation i
            WHERE i.recipient.id = :userId
              AND i.status = it.unicam.cs.hackhub.model.enums.InvitationState.PENDING
            """)
    List<Invitation> findPendingByUser(@Param("userId") Long userId);
}
