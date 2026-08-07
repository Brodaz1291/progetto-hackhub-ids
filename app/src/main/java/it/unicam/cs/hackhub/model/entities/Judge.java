package it.unicam.cs.hackhub.model.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("JUDGE")
public class Judge extends StaffParticipation {
}
