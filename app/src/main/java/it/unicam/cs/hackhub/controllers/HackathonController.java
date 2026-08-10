package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.HackathonDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.HackathonService;
import it.unicam.cs.hackhub.controllers.requests.CreateHackathonRequest;
import it.unicam.cs.hackhub.controllers.requests.UpdateHackathonRequest;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hackathons")
public class HackathonController {

    private final HackathonService hackathonService;
    private final DTOMapper dtoMapper;

    public HackathonController(HackathonService hackathonService, DTOMapper dtoMapper) {
        this.hackathonService = hackathonService;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Creates a hackathon on behalf of an organizer.
     *
     * NOTE: the identity of the organizer travels as an explicit parameter, as the model
     * prescribes, instead of being read from the security context.
     */
    @PostMapping
    public HackathonDTO createHackathon(@RequestBody CreateHackathonRequest req,
                                        @RequestParam Long organizerId) {
        Hackathon createdHackathon = hackathonService.createHackathon(req, organizerId);
        return dtoMapper.toDTO(createdHackathon);
    }

    /**
     * Replaces the information of a hackathon still open to registrations.
     */
    @PutMapping("/{hackathonId}")
    public HackathonDTO updateHackathon(@PathVariable Long hackathonId,
                                        @RequestBody UpdateHackathonRequest req) {
        Hackathon updatedHackathon = hackathonService.updateHackathon(hackathonId, req);
        return dtoMapper.toDTO(updatedHackathon);
    }
}
