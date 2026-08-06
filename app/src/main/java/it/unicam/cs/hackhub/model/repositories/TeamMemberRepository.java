package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByUserId(Long userId);
}
