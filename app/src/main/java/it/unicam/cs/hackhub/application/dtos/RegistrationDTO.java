package it.unicam.cs.hackhub.application.dtos;

import it.unicam.cs.hackhub.model.enums.RegistrationState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDTO {

    private Long id;

    private String teamName;

    private String hackathonName;

    private LocalDateTime registrationDate;

    private RegistrationState state;
}
