package it.unicam.cs.hackhub.controllers.requests;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Carries only the information proper to a hackathon: the staff is not editable here,
 * it changes through addMentor, removeMentor and replaceJudge.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateHackathonRequest {

    private String name;

    private String rules;

    private String location;

    private BigDecimal prize;

    private LocalDateTime registrationDeadline;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private int maxTeamSize;
}
