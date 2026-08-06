package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}
