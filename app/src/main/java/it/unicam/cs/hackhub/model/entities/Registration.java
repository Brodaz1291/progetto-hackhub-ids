package it.unicam.cs.hackhub.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unicam.cs.hackhub.model.enums.RegistrationState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    private RegistrationState state;

    private String disqualificationReason;

    @ManyToOne
    @JoinColumn(name = "hackathon_id")
    @JsonIgnore
    private Hackathon hackathon;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    private Submission submission;

    public Registration(LocalDateTime registrationDate, RegistrationState state) {
        this.registrationDate = registrationDate;
        this.state = state;
    }
}
