package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Call;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRepository extends JpaRepository<Call, Long> {
}
