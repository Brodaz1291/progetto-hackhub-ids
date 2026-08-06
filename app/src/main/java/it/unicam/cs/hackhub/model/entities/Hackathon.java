package it.unicam.cs.hackhub.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Hackathon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String rules;

    private String location;

    private BigDecimal prize;

    private LocalDateTime registrationDeadline;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private int maxTeamSize;

    @Enumerated(EnumType.STRING)
    private HackathonState state;

    @OneToMany(mappedBy = "hackathon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations = new ArrayList<>();

    @OneToMany(mappedBy = "hackathon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StaffParticipation> staff = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "winner_registration_id")
    @JsonIgnore
    private Registration winner;

    public Hackathon(String name, String rules, String location, BigDecimal prize,
                     LocalDateTime registrationDeadline, LocalDateTime startDate,
                     LocalDateTime endDate, int maxTeamSize, HackathonState state) {
        this.name = name;
        this.rules = rules;
        this.location = location;
        this.prize = prize;
        this.registrationDeadline = registrationDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxTeamSize = maxTeamSize;
        this.state = state;
    }
}
