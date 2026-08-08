package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.controllers.requests.CreateHackathonRequest;
import it.unicam.cs.hackhub.designPatterns.HackathonBuilder;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Judge;
import it.unicam.cs.hackhub.model.entities.Mentor;
import it.unicam.cs.hackhub.model.entities.Organizer;
import it.unicam.cs.hackhub.model.entities.StaffParticipation;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.repositories.HackathonRepository;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class HackathonService {

    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ObjectProvider<HackathonBuilder> builderProvider;

    public HackathonService(HackathonRepository hackathonRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            ObjectProvider<HackathonBuilder> builderProvider) {
        this.hackathonRepository = hackathonRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.builderProvider = builderProvider;
    }

    /**
     * Creates a hackathon in registration phase, assigns its staff and notifies it.
     */
    public Hackathon createHackathon(CreateHackathonRequest req, Long organizerId) {
        if (!checkInformation(req)) {
            throw new IllegalArgumentException("Invalid hackathon information");
        }
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("Organizer not found: " + organizerId));
        User judge = userRepository.findById(req.getJudgeId())
                .orElseThrow(() -> new IllegalArgumentException("Judge not found: " + req.getJudgeId()));
        List<User> mentors = userRepository.findAllById(req.getMentorIds());
        if (mentors.size() != req.getMentorIds().size()) {
            throw new IllegalArgumentException("Unknown or duplicated mentors: " + req.getMentorIds());
        }

        // the builder is prototype-scoped: a fresh instance per creation, since it keeps
        // state between one setter and the next
        HackathonBuilder builder = builderProvider.getObject();
        builder.setHackathonInformation(req);
        Hackathon hackathon = builder.getResult();
        hackathon.setState(HackathonState.REGISTRATION);

        Organizer organizerParticipation = new Organizer();
        organizerParticipation.setUser(organizer);
        Judge judgeParticipation = new Judge();
        judgeParticipation.setUser(judge);
        List<Mentor> mentorParticipations = mentors.stream()
                .map(mentorUser -> {
                    Mentor mentorParticipation = new Mentor();
                    mentorParticipation.setUser(mentorUser);
                    return mentorParticipation;
                })
                .toList();

        List<StaffParticipation> staff = new ArrayList<>(mentorParticipations);
        staff.add(organizerParticipation);
        staff.add(judgeParticipation);
        // both ends of the association are needed: the list drives the cascade, the
        // participation owns the foreign key
        staff.forEach(participation -> participation.setHackathon(hackathon));
        hackathon.getStaff().addAll(staff);

        Hackathon createdHackathon = hackathonRepository.save(hackathon);
        notificationService.notifyStaff(judgeParticipation, mentorParticipations);
        return createdHackathon;
    }

    private boolean checkInformation(CreateHackathonRequest req) {
        if (!StringUtils.hasText(req.getName()) || !StringUtils.hasText(req.getRules())
                || !StringUtils.hasText(req.getLocation())) {
            return false;
        }
        if (req.getRegistrationDeadline() == null || req.getStartDate() == null || req.getEndDate() == null) {
            return false;
        }
        if (req.getRegistrationDeadline().isAfter(req.getStartDate())
                || !req.getStartDate().isBefore(req.getEndDate())) {
            return false;
        }
        if (req.getPrize() == null || req.getPrize().signum() < 0 || req.getMaxTeamSize() <= 0) {
            return false;
        }
        return req.getJudgeId() != null && req.getMentorIds() != null && !req.getMentorIds().isEmpty();
    }
}
