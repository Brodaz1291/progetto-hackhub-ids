package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);
}
