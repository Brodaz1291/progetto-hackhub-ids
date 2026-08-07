package it.unicam.cs.hackhub.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public abstract class StaffParticipation extends Participation {

    @ManyToOne
    @JoinColumn(name = "hackathon_id")
    @JsonIgnore
    private Hackathon hackathon;
}
