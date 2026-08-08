package it.unicam.cs.hackhub.model.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("JUDGE")
@NoArgsConstructor
public class Judge extends StaffParticipation {

    public Judge(User user, Hackathon hackathon) {
        super(user, hackathon);
    }
}
