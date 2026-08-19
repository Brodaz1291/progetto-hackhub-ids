package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.ConflictException;
import it.unicam.cs.hackhub.application.exceptions.NotFoundException;
import it.unicam.cs.hackhub.application.exceptions.ValidationException;
import it.unicam.cs.hackhub.designPatterns.adapter.PaymentProcessor;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import it.unicam.cs.hackhub.model.entities.Payment;
import it.unicam.cs.hackhub.model.entities.Registration;
import it.unicam.cs.hackhub.model.entities.Team;
import it.unicam.cs.hackhub.model.enums.HackathonState;
import it.unicam.cs.hackhub.model.repositories.HackathonRepository;
import it.unicam.cs.hackhub.model.repositories.PaymentRepository;
import it.unicam.cs.hackhub.model.repositories.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final HackathonRepository hackathonRepository;
    private final RegistrationRepository registrationRepository;
    private final PaymentProcessor paymentProcessor;
    private final NotificationService notificationService;
    private final Clock clock;

    public PaymentService(PaymentRepository paymentRepository,
                          HackathonRepository hackathonRepository,
                          RegistrationRepository registrationRepository,
                          PaymentProcessor paymentProcessor,
                          NotificationService notificationService,
                          Clock clock) {
        this.paymentRepository = paymentRepository;
        this.hackathonRepository = hackathonRepository;
        this.registrationRepository = registrationRepository;
        this.paymentProcessor = paymentProcessor;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * Transfers the prize of a concluded hackathon to the winning team and records the payment,
     * so that the same prize cannot be paid twice.
     *
     * Every check comes before the transfer because the transfer is the one step that cannot be
     * undone: the transaction rolls back what the platform has written, not what the bank has
     * moved. Recording the payment right after is what makes the prize paid as far as the
     * platform is concerned, and what checkNotAlreadyPaid will read next time.
     */
    @Transactional
    public Payment payPrize(Long hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new NotFoundException("Hackathon not found: " + hackathonId));
        if (!checkHackathonConcluded(hackathon)) {
            throw new ValidationException("The prize can be paid only after the winner has been proclaimed");
        }

        Registration winner = registrationRepository.findWinnerByHackathonId(hackathonId)
                .orElseThrow(() -> new NotFoundException(
                        "Hackathon " + hackathonId + " was concluded with no winner proclaimed: there is no prize to pay"));
        if (!checkNotAlreadyPaid(winner)) {
            throw new ConflictException("The prize of hackathon " + hackathonId + " has already been paid");
        }

        Team team = winner.getTeam();
        String externalId = paymentProcessor.transfer(team.getIban(), hackathon.getPrize());
        Payment payment = paymentRepository.save(
                new Payment(winner, hackathon.getPrize(), LocalDateTime.now(clock), externalId));
        notificationService.notifyTeam(team);

        return payment;
    }

    /**
     * The prize belongs to a hackathon that is over: while the event runs there is no winner to
     * pay, and the conclusion is the act that proclaims one.
     */
    private boolean checkHackathonConcluded(Hackathon hackathon) {
        return hackathon.getState() == HackathonState.CONCLUDED;
    }

    /**
     * A prize is paid once: the payment recorded on the winning registration is the trace that
     * says the money has already left.
     */
    private boolean checkNotAlreadyPaid(Registration registration) {
        return paymentRepository.findByRegistrationId(registration.getId()).isEmpty();
    }
}
