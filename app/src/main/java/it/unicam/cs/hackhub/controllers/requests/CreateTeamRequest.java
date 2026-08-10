package it.unicam.cs.hackhub.controllers.requests;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateTeamRequest {

    private String name;

    private String iban;
}
