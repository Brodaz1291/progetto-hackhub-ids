package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Hackathon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HackathonRepository extends JpaRepository<Hackathon, Long> {

    /**
     * The staff is fetched together with the hackathon: it is part of every response that
     * describes one, and reading it afterwards would find the persistence context already
     * closed. The same applies to the two queries below.
     */
    @Query("""
            SELECT h FROM Hackathon h
            LEFT JOIN FETCH h.staff
            """)
    List<Hackathon> findAll();

    @Query("""
            SELECT h FROM Hackathon h
            LEFT JOIN FETCH h.staff
            WHERE h.state = it.unicam.cs.hackhub.model.enums.HackathonState.REGISTRATION
            """)
    List<Hackathon> findOpenForRegistration();

    @Query("""
            SELECT h FROM Hackathon h
            LEFT JOIN FETCH h.staff
            WHERE h.id = :hackathonId
            """)
    Optional<Hackathon> findById(@Param("hackathonId") Long hackathonId);
}
