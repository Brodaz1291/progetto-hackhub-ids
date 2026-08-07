package it.unicam.cs.hackhub.model.repositories;

import it.unicam.cs.hackhub.model.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRegistrationId(Long registrationId);
}
