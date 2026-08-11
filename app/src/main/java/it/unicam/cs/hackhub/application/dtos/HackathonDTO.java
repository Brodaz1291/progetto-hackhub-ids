package it.unicam.cs.hackhub.application.dtos;

import it.unicam.cs.hackhub.model.enums.HackathonState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HackathonDTO {

    private Long id;

    private String name;

    private String location;

    private BigDecimal prize;

    private String rules;

    private LocalDateTime registrationDeadline;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private int maxTeamSize;

    private HackathonState state;

    private String winnerTeamName;

    private String organizerUsername;

    private String judgeUsername;

    private List<String> mentorUsernames;
}
