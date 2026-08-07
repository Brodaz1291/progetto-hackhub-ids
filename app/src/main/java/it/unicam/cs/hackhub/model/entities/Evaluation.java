package it.unicam.cs.hackhub.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int score;

    private String judgment;

    public Evaluation(int score, String judgment) {
        this.score = score;
        this.judgment = judgment;
    }
}
