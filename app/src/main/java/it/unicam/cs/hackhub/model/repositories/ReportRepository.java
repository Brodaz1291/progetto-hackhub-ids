package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r WHERE r.registration.hackathon.id = :hackathonId")
    List<Report> findByHackathonId(@Param("hackathonId") Long hackathonId);
}
