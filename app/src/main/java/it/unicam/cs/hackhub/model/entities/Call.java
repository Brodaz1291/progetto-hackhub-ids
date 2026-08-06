package it.unicam.cs.hackhub.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateTime;

    private String externalId;

    @ManyToOne
    @JoinColumn(name = "mentor_id")
    @JsonIgnore
    private Mentor mentor;

    @OneToOne
    @JoinColumn(name = "support_request_id")
    private SupportRequest supportRequest;

    public Call(LocalDateTime dateTime, String externalId) {
        this.dateTime = dateTime;
        this.externalId = externalId;
    }
}
