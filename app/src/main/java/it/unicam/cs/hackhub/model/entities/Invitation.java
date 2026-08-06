package it.unicam.cs.hackhub.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unicam.cs.hackhub.model.enums.InvitationState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InvitationState status;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    @JsonIgnore
    private TeamMember sender;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    public Invitation(InvitationState status) {
        this.status = status;
    }
}
