package it.unicam.cs.hackhub.controllers.requests;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateHackathonRequest {

    private String name;

    private String rules;

    private String location;

    private BigDecimal prize;

    private LocalDateTime registrationDeadline;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private int maxTeamSize;

    private Long judgeId;

    private List<Long> mentorIds;
}
