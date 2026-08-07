package it.unicam.cs.hackhub.application.dtos;

import it.unicam.cs.hackhub.model.enums.InvitationState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDTO {

    private Long id;

    private String teamName;

    private String senderUsername;

    private InvitationState status;
}
