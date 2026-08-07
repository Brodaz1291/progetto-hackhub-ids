package it.unicam.cs.hackhub.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDTO {

    private Long id;

    private String title;

    private String description;

    private String link;

    private LocalDateTime submissionDate;

    private String teamName;

    private Integer score;

    private String judgment;
}
