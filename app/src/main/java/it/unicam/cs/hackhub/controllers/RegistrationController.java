package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.RegistrationDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.RegistrationService;
import it.unicam.cs.hackhub.model.entities.Registration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final DTOMapper dtoMapper;

    public RegistrationController(RegistrationService registrationService, DTOMapper dtoMapper) {
        this.registrationService = registrationService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    public RegistrationDTO registerTeam(@RequestParam Long hackathonId, @RequestParam Long teamId) {
        Registration registration = registrationService.registerTeam(hackathonId, teamId);
        return dtoMapper.toDTO(registration);
    }

    /**
     * Excludes a team from the event it is taking part in.
     *
     * NOTE: the disqualification is a PUT because it marks the registration without removing
     * it, and the reason travels as a query parameter as the model prescribes.
     */
    @PutMapping("/{registrationId}/disqualification")
    public void disqualifyTeam(@PathVariable Long registrationId, @RequestParam String reason) {
        registrationService.disqualifyTeam(registrationId, reason);
    }
}
