package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.ConflictException;
import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.controllers.requests.CreateHackathonRequest;
import it.unicam.cs.hackhub.controllers.requests.UpdateHackathonRequest;
import it.unicam.cs.hackhub.designPatterns.HackathonBuilder;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Judge;
import it.unicam.cs.hackhub.model.entities.Mentor;
import it.unicam.cs.hackhub.model.entities.Organizer;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Submission;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.repositories.HackathonRepository;
import it.unicam.cs.hackhub.model.repositories.ParticipationRepository;
import it.unicam.cs.hackhub.model.repositories.SubmissionRepository;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@Service
public class HackathonService {

    private final HackathonRepository hackathonRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final SubmissionRepository submissionRepository;
    private final NotificationService notificationService;
    private final ObjectProvider<HackathonBuilder> builderProvider;
    private final HackathonLifecycle hackathonLifecycle;

    public HackathonService(HackathonRepository hackathonRepository,
                            UserRepository userRepository,
                            ParticipationRepository participationRepository,
                            SubmissionRepository submissionRepository,
                            NotificationService notificationService,
                            ObjectProvider<HackathonBuilder> builderProvider,
                            HackathonLifecycle hackathonLifecycle) {
        this.hackathonRepository = hackathonRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.submissionRepository = submissionRepository;
        this.notificationService = notificationService;
        this.builderProvider = builderProvider;
        this.hackathonLifecycle = hackathonLifecycle;
    }

    public List<Hackathon> getAllHackathons() {
        List<Hackathon> hackathons = hackathonRepository.findAll();
        hackathons.forEach(hackathonLifecycle::refreshState);
        return hackathons;
    }

    /**
     * Returns the hackathons that are still in their registration phase, the only ones a
     * team can enrol in. The query selects the ones the database still records as open, but
     * the phase is decided by the dates: the ones the refresh has just moved on are left out.
     */
    public List<Hackathon> getOpenHackathons() {
        List<Hackathon> hackathons = hackathonRepository.findOpenForRegistration();
        hackathons.forEach(hackathonLifecycle::refreshState);
        return hackathons.stream()
                .filter(hackathon -> hackathon.getState() == HackathonState.REGISTRATION)
                .toList();
    }

    public Hackathon getHackathon(Long hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        hackathonLifecycle.refreshState(hackathon);
        return hackathon;
    }

    /**
     * Creates a hackathon in registration phase, assigns its staff and notifies it.
     */
    public Hackathon createHackathon(CreateHackathonRequest req, Long organizerId) {
        if (!checkInformation(req)) {
            throw new ValidationException("Invalid hackathon information");
        }
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new NotFoundException("Organizer not found: " + organizerId));
        User judge = userRepository.findById(req.getJudgeId())
                .orElseThrow(() -> new NotFoundException("Judge not found: " + req.getJudgeId()));
        List<User> mentors = userRepository.findAllById(req.getMentorIds());
        if (mentors.size() != req.getMentorIds().size()) {
            throw new ValidationException("Unknown or duplicated mentors: " + req.getMentorIds());
        }

        // the whole staff is checked before anything is built: assigning somebody already busy
        // would create here a participation that addMentor and replaceJudge would refuse a
        // moment later
        List<User> assignedStaff = Stream.concat(Stream.of(organizer, judge), mentors.stream()).toList();
        for (User staffMember : assignedStaff) {
            if (!checkNoActiveParticipation(staffMember)) {
                throw new ConflictException(
                        "User already takes part in something else: " + staffMember.getUsername());
            }
        }

        // the builder is prototype-scoped: a fresh instance per creation, since it keeps
        // state between one setter and the next
        HackathonBuilder builder = builderProvider.getObject();
        builder.setHackathonInformation(req);
        Hackathon hackathon = builder.getResult();
        hackathon.setState(HackathonState.REGISTRATION);

        Organizer organizerParticipation = new Organizer(organizer, hackathon);
        Judge judgeParticipation = new Judge(judge, hackathon);
        List<Mentor> mentorParticipations = mentors.stream()
                .map(mentorUser -> new Mentor(mentorUser, hackathon))
                .toList();

        // the participations own the foreign key, but they also have to appear in the list
        // for the cascade to persist them
        hackathon.getStaff().add(organizerParticipation);
        hackathon.getStaff().add(judgeParticipation);
        hackathon.getStaff().addAll(mentorParticipations);

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

    /**
     * Updates the information of a hackathon still open to registrations.
     */
    public Hackathon updateHackathon(Long hackathonId, UpdateHackathonRequest req) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        hackathonLifecycle.refreshState(hackathon);
        // the phase is checked before the data: once registrations are closed the
        // information is settled with the enrolled teams, so validating it is pointless
        if (!checkHackathonInRegistration(hackathon)) {
            throw new ValidationException("A hackathon can be updated only during its registration phase");
        }
        if (!checkUpdateInformation(req)) {
            throw new ValidationException("Invalid hackathon update information");
        }
        applyChanges(hackathon, req);
        return hackathonRepository.save(hackathon);
    }

    private boolean checkHackathonInRegistration(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.REGISTRATION;
    }

    private boolean checkUpdateInformation(UpdateHackathonRequest req) {
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
        return req.getPrize() != null && req.getPrize().signum() >= 0 && req.getMaxTeamSize() > 0;
    }

    private void applyChanges(Hackathon hackathon, UpdateHackathonRequest req) {
        hackathon.setName(req.getName());
        hackathon.setRules(req.getRules());
        hackathon.setLocation(req.getLocation());
        hackathon.setPrize(req.getPrize());
        hackathon.setRegistrationDeadline(req.getRegistrationDeadline());
        hackathon.setStartDate(req.getStartDate());
        hackathon.setEndDate(req.getEndDate());
        hackathon.setMaxTeamSize(req.getMaxTeamSize());
    }

    /**
     * Assigns a further mentor to a hackathon and notifies them.
     */
    @Transactional
    public Mentor addMentor(Long hackathonId, String username) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        hackathonLifecycle.refreshState(hackathon);
        // the phase is checked before the user: the staff of an event that is over is settled,
        // and no candidate makes it assignable again
        if (!checkHackathonNotConcluded(hackathon)) {
            throw new ValidationException("A mentor cannot be added to a concluded hackathon");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        if (!checkNoActiveParticipation(user)) {
            throw new ConflictException("User already takes part in something else: " + username);
        }

        Mentor mentor = new Mentor(user, hackathon);
        // the participation owns the foreign key, but it also has to appear in the list for
        // the cascade to persist it
        hackathon.getStaff().add(mentor);
        hackathonRepository.save(hackathon);

        notificationService.notifyMentor(mentor);
        return mentor;
    }

    /**
     * Removes a mentor from a hackathon and notifies them. The calls they had planned lapse
     * with the participation.
     */
    @Transactional
    public void removeMentor(Long hackathonId, String username) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        hackathonLifecycle.refreshState(hackathon);
        if (!checkHackathonNotConcluded(hackathon)) {
            throw new ValidationException("A mentor cannot be removed from a concluded hackathon");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        // looking the mentor up inside the staff of this hackathon makes both the type and
        // the membership checks implicit
        Mentor mentor = hackathon.getStaff().stream()
                .filter(Mentor.class::isInstance)
                .map(Mentor.class::cast)
                .filter(staffMentor -> staffMentor.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        username + " is not a mentor of hackathon " + hackathonId));
        if (!checkNotLastMentor(hackathon)) {
            throw new ValidationException("The last mentor cannot be removed, only replaced");
        }

        // the notice goes out before the removal: afterwards the mentor no longer exists
        notificationService.notifyMentor(mentor);
        // removing the participation from the list is what deletes it: orphanRemoval takes
        // care of the row and, in cascade, of the calls the mentor had planned
        hackathon.getStaff().removeIf(staff -> staff.getId().equals(mentor.getId()));
        hackathonRepository.save(hackathon);
    }

    /**
     * Replaces the judge of a hackathon with another user and notifies both. Removal and
     * assignment happen together: a hackathon is judged by exactly one judge, so it must
     * never be left without one.
     */
    @Transactional
    public Judge replaceJudge(Long hackathonId, String username) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        hackathonLifecycle.refreshState(hackathon);
        // the phase is checked before the user: once the evaluation is under way the
        // replacement is impossible anyway, whoever the candidate is
        if (!checkEvaluationNotStarted(hackathon)) {
            throw new ValidationException("The judge cannot be replaced once the evaluation has started");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        if (!checkNoActiveParticipation(user)) {
            throw new ConflictException("User already takes part in something else: " + username);
        }
        Judge oldJudge = hackathon.getStaff().stream()
                .filter(Judge.class::isInstance)
                .map(Judge.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hackathon " + hackathonId + " has no judge"));

        // the notice goes out before the removal: afterwards the participation no longer exists
        notificationService.notifyJudge(oldJudge);
        // removing the participation from the list is what deletes it: orphanRemoval takes
        // care of the row
        hackathon.getStaff().removeIf(staff -> staff.getId().equals(oldJudge.getId()));

        Judge newJudge = new Judge(user, hackathon);
        hackathon.getStaff().add(newJudge);
        hackathonRepository.save(hackathon);

        notificationService.notifyJudge(newJudge);
        return newJudge;
    }

    /**
     * The staff of a concluded event is the record of who ran it: the reports collected and the
     * calls held point at those participations, so the roster is not touched any more.
     */
    private boolean checkHackathonNotConcluded(Hackathon hackathon) {
        return hackathon.getState() != HackathonState.CONCLUDED;
    }

    /**
     * The evaluations already released belong to the judge who expressed them through their
     * participation, so the judge can only be replaced before the evaluation phase.
     */
    private boolean checkEvaluationNotStarted(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.REGISTRATION
                || hackathon.getState() == HackathonState.RUNNING;
    }

    /**
     * A user takes part in one thing at a time: either a team membership or a staff role in
     * a hackathon that is still running. The query looks at both branches of the hierarchy.
     */
    private boolean checkNoActiveParticipation(User user) {
        return participationRepository.findActiveByUserId(user.getId()).isEmpty();
    }

    /**
     * A hackathon is mentored by at least one mentor, so the last one can only be replaced:
     * another mentor has to be added before removing them.
     */
    private boolean checkNotLastMentor(Hackathon hackathon) {
        long mentorCount = hackathon.getStaff().stream()
                .filter(Mentor.class::isInstance)
                .count();
        return mentorCount > 1;
    }

    /**
     * Proclaims the winning team of a hackathon and notifies it. Registering the winner and
     * concluding the event are a single act: a concluded hackathon with nobody proclaimed is a
     * state the model does not admit, except when there is nothing to choose from.
     *
     * The winner is looked up among the evaluated submissions rather than by identifier alone:
     * finding it there is what makes it certain that the registration belongs to this
     * hackathon, that the team was not disqualified, that it handed in and that it was judged.
     */
    @Transactional
    public Hackathon proclaimWinner(Long hackathonId, Long registrationId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        hackathonLifecycle.refreshState(hackathon);
        if (!checkHackathonInEvaluation(hackathon)) {
            throw new ValidationException("The winner can be proclaimed only during the evaluation phase");
        }
        if (!checkAllSubmissionsEvaluated(hackathon)) {
            throw new ValidationException("The winner can be proclaimed only once every submission has been evaluated");
        }
        if (!checkHasEvaluatedSubmissions(hackathon)) {
            applyConclusion(hackathon, null);
            return hackathonRepository.save(hackathon);
        }
        if (registrationId == null) {
            throw new ValidationException(
                    "Hackathon " + hackathonId + " has evaluated submissions: the winner has to be selected");
        }

        Registration winner = submissionRepository.findEvaluatedByHackathonId(hackathonId).stream()
                .map(Submission::getRegistration)
                .filter(registration -> registration.getId().equals(registrationId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Registration " + registrationId
                        + " is not among the evaluated submissions of hackathon " + hackathonId));
        applyConclusion(hackathon, winner);
        Hackathon concludedHackathon = hackathonRepository.save(hackathon);

        notificationService.notifyTeam(winner.getTeam());
        return concludedHackathon;
    }

    /**
     * The proclamation closes the evaluation phase: before it the scores are not all in, after
     * it the hackathon is concluded and its result is settled. Since the concluded phase is
     * terminal, this is also what makes a second proclamation impossible.
     */
    private boolean checkHackathonInEvaluation(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.EVALUATION;
    }

    /**
     * The winner is chosen against complete information: as long as one submission is left
     * without a score the comparison between the teams is not settled. The submissions of the
     * disqualified teams are left out, since nobody is going to evaluate them.
     *
     * With no submission to consider the check is vacuously true, and this is what makes the
     * two checks fit together: the hackathon where everyone was disqualified or nobody handed
     * in passes here and is caught by checkHasEvaluatedSubmissions, which concludes it without
     * a winner instead of blocking it.
     */
    private boolean checkAllSubmissionsEvaluated(Hackathon hackathon) {
        return submissionRepository.findByHackathonIdNotDisqualified(hackathon.getId()).stream()
                .allMatch(submission -> submission.getEvaluation() != null);
    }

    /**
     * Distinguishes the two outcomes of the proclamation: a winner chosen among the evaluated
     * submissions, or the bare conclusion of an event where there is none to choose.
     */
    private boolean checkHasEvaluatedSubmissions(Hackathon hackathon) {
        return !submissionRepository.findEvaluatedByHackathonId(hackathon.getId()).isEmpty();
    }

    /**
     * Winner and conclusion are written together because they are the same act, and there is
     * no operation that concludes a hackathon on its own. The winner is null when no submission
     * has been evaluated: the only case in which the event closes with nobody proclaimed.
     */
    private void applyConclusion(Hackathon hackathon, Registration winner) {
        hackathon.setWinner(winner);
        hackathon.setState(HackathonState.CONCLUDED);
    }
}
