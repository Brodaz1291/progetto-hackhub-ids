package it.unicam.cs.hackhub.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reason;

    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "registration_id")
    private Registration registration;

    public Report(String reason, LocalDateTime date) {
        this.reason = reason;
        this.date = date;
    }
}
