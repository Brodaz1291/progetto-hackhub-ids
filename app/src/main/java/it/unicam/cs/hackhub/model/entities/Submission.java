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
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String link;

    private LocalDateTime submissionDate;

    @OneToOne
    @JoinColumn(name = "registration_id")
    @JsonIgnore
    private Registration registration;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "evaluation_id")
    private Evaluation evaluation;

    public Submission(Registration registration, String title, String description, String link, LocalDateTime submissionDate) {
        this.registration = registration;
        this.title = title;
        this.description = description;
        this.link = link;
        this.submissionDate = submissionDate;
    }
}
