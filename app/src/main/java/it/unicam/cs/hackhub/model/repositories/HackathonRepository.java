package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Hackathon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HackathonRepository extends JpaRepository<Hackathon, Long> {

    @Query("""
            SELECT h FROM Hackathon h
            WHERE h.state = it.unicam.cs.hackhub.model.enums.HackathonState.REGISTRATION
            """)
    List<Hackathon> findOpenForRegistration();
}
