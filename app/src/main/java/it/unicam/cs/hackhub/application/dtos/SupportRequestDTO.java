package it.unicam.cs.hackhub.application.dtos;

import it.unicam.cs.hackhub.model.enums.SupportRequestState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequestDTO {

    private Long id;

    private String description;

    private SupportRequestState status;

    private String teamName;

    private LocalDateTime callDateTime;
}
