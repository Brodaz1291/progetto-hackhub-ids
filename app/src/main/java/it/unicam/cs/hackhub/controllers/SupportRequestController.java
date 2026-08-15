package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.SupportRequestDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.CallService;
import it.unicam.cs.hackhub.application.services.SupportRequestService;
import it.unicam.cs.hackhub.model.entities.Call;
import it.unicam.cs.hackhub.model.entities.SupportRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/support-requests")
public class SupportRequestController {

    private final SupportRequestService supportRequestService;
    private final CallService callService;
    private final DTOMapper dtoMapper;

    public SupportRequestController(SupportRequestService supportRequestService,
                                    CallService callService,
                                    DTOMapper dtoMapper) {
        this.supportRequestService = supportRequestService;
        this.callService = callService;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Sends a support request to the mentors of the hackathon the team is registered to.
     */
    @PostMapping
    public SupportRequestDTO sendSupportRequest(@RequestParam Long registrationId,
                                                @RequestParam String description) {
        SupportRequest supportRequest = supportRequestService.sendSupportRequest(registrationId, description);
        return dtoMapper.toDTO(supportRequest);
    }

    /**
     * Returns the requests of a hackathon a mentor still has to take over.
     */
    @GetMapping("/pending")
    public List<SupportRequestDTO> getPendingSupportRequests(@RequestParam Long hackathonId) {
        List<SupportRequest> pendingRequests = supportRequestService.getPendingRequests(hackathonId);
        return pendingRequests.stream()
                .map(dtoMapper::toDTO)
                .toList();
    }

    /**
     * Returns the requests sent by a team, with the state each one is in.
     */
    @GetMapping
    public List<SupportRequestDTO> getTeamSupportRequests(@RequestParam Long registrationId) {
        List<SupportRequest> teamRequests = supportRequestService.getTeamRequests(registrationId);
        return teamRequests.stream()
                .map(dtoMapper::toDTO)
                .toList();
    }

    @GetMapping("/{supportRequestId}")
    public SupportRequestDTO getSupportRequest(@PathVariable Long supportRequestId) {
        return dtoMapper.toDTO(supportRequestService.getSupportRequest(supportRequestId));
    }

    /**
     * Plans the call a mentor holds with the team that asked for support.
     *
     * NOTE: what comes back is the request the call answers, not the call: the slot just
     * booked already travels in its projection, and reading the request back shows that it
     * has left the pending state.
     */
    @PostMapping("/{supportRequestId}/call")
    public SupportRequestDTO scheduleCall(@PathVariable Long supportRequestId,
                                          @RequestParam Long mentorId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                          LocalDateTime dateTime) {
        Call call = callService.scheduleCall(supportRequestId, mentorId, dateTime);
        return dtoMapper.toDTO(call.getSupportRequest());
    }
}
