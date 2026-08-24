package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.HackathonDTO;
import it.unicam.cs.hackhub.application.dtos.UserDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.HackathonService;
import it.unicam.cs.hackhub.controllers.requests.CreateHackathonRequest;
import it.unicam.cs.hackhub.controllers.requests.UpdateHackathonRequest;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Judge;
import it.unicam.cs.hackhub.model.entities.Mentor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hackathons")
public class HackathonController {

    private final HackathonService hackathonService;
    private final DTOMapper dtoMapper;

    public HackathonController(HackathonService hackathonService, DTOMapper dtoMapper) {
        this.hackathonService = hackathonService;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping
    public List<HackathonDTO> getHackathons() {
        List<Hackathon> hackathons = hackathonService.getAllHackathons();
        return hackathons.stream().map(dtoMapper::toDTO).toList();
    }

    @GetMapping("/open")
    public List<HackathonDTO> getOpenHackathons() {
        List<Hackathon> hackathons = hackathonService.getOpenHackathons();
        return hackathons.stream().map(dtoMapper::toDTO).toList();
    }

    /**
     * Lists the hackathons for a visitor who is not authenticated. The hackathons read are
     * the same ones {@link #getHackathons()} returns: what changes is only the conversion,
     * because deciding how much of a hackathon a caller may see belongs to the presentation
     * layer, not to the domain.
     */
    @GetMapping("/public")
    public List<HackathonDTO> getPublicHackathons() {
        List<Hackathon> hackathons = hackathonService.getAllHackathons();
        return hackathons.stream().map(dtoMapper::toPublicDTO).toList();
    }

    @GetMapping("/{hackathonId}")
    public HackathonDTO getHackathon(@PathVariable Long hackathonId) {
        Hackathon hackathon = hackathonService.getHackathon(hackathonId);
        return dtoMapper.toDTO(hackathon);
    }

    /**
     * Creates a hackathon on behalf of an organizer.
     *
     * NOTE: the identity of the organizer travels as an explicit parameter, as the model
     * prescribes, instead of being read from the security context.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HackathonDTO createHackathon(@RequestBody CreateHackathonRequest req,
                                        @RequestParam Long organizerId) {
        Hackathon createdHackathon = hackathonService.createHackathon(req, organizerId);
        return dtoMapper.toDTO(createdHackathon);
    }

    @PutMapping("/{hackathonId}")
    public HackathonDTO updateHackathon(@PathVariable Long hackathonId,
                                        @RequestBody UpdateHackathonRequest req) {
        Hackathon updatedHackathon = hackathonService.updateHackathon(hackathonId, req);
        return dtoMapper.toDTO(updatedHackathon);
    }

    /**
     * NOTE: the response describes the user who became a mentor, not the participation:
     * what the client needs to know is who now mentors the hackathon.
     */
    @PostMapping("/{hackathonId}/mentors")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO addMentor(@PathVariable Long hackathonId, @RequestParam String username) {
        Mentor mentor = hackathonService.addMentor(hackathonId, username);
        return dtoMapper.toDTO(mentor.getUser());
    }

    @DeleteMapping("/{hackathonId}/mentors")
    public void removeMentor(@PathVariable Long hackathonId, @RequestParam String username) {
        hackathonService.removeMentor(hackathonId, username);
    }

    @PutMapping("/{hackathonId}/judge")
    public UserDTO replaceJudge(@PathVariable Long hackathonId, @RequestParam String username) {
        Judge judge = hackathonService.replaceJudge(hackathonId, username);
        return dtoMapper.toDTO(judge.getUser());
    }

    /**
     * NOTE: the registration is optional because a hackathon where no submission has been
     * evaluated is concluded without a winner, and there the client has nobody to name. The
     * response describes the concluded hackathon since that is the only way to tell the two
     * outcomes apart: it carries both the state and the name of the winning team.
     */
    @PutMapping("/{hackathonId}/winner")
    public HackathonDTO proclaimWinner(@PathVariable Long hackathonId,
                                       @RequestParam(required = false) Long registrationId) {
        Hackathon concludedHackathon = hackathonService.proclaimWinner(hackathonId, registrationId);
        return dtoMapper.toDTO(concludedHackathon);
    }
}
