package it.unicam.cs.hackhub.model.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ORGANIZER")
@NoArgsConstructor
public class Organizer extends StaffParticipation {

    public Organizer(User user, Hackathon hackathon) {
        super(user, hackathon);
    }
}
