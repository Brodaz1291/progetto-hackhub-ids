package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.PaymentDTO;
import it.unicam.cs.hackhub.application.dtos.RegistrationDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.PaymentService;
import it.unicam.cs.hackhub.application.services.RegistrationService;
import it.unicam.cs.hackhub.model.entities.Payment;
import it.unicam.cs.hackhub.model.entities.Registration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final PaymentService paymentService;
    private final DTOMapper dtoMapper;

    public RegistrationController(RegistrationService registrationService,
                                  PaymentService paymentService,
                                  DTOMapper dtoMapper) {
        this.registrationService = registrationService;
        this.paymentService = paymentService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationDTO registerTeam(@RequestParam Long hackathonId, @RequestParam Long teamId) {
        Registration registration = registrationService.registerTeam(hackathonId, teamId);
        return dtoMapper.toDTO(registration);
    }

    /**
     * Lists the teams enrolled in a hackathon. The organizer and the mentor read it to choose
     * which registration to disqualify or to report: both operations take a registration id,
     * and this is where it comes from.
     */
    @GetMapping
    public List<RegistrationDTO> getRegistrations(@RequestParam Long hackathonId) {
        List<Registration> registrations = registrationService.getRegistrations(hackathonId);
        return registrations.stream()
                .map(dtoMapper::toDTO)
                .toList();
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

    /**
     * Pays the prize of a concluded hackathon to the registration that won it.
     *
     * NOTE: the hackathon travels as a query parameter because the model asks for it alone:
     * which registration is being paid is the winner of that event, not a choice of the
     * client. The response leaves the identifier of the external transaction out.
     */
    @PostMapping("/prize")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDTO payPrize(@RequestParam Long hackathonId) {
        Payment payment = paymentService.payPrize(hackathonId);
        return dtoMapper.toDTO(payment);
    }
}
