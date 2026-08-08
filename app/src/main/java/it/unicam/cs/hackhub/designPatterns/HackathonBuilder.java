package it.unicam.cs.hackhub.designPatterns;

import it.unicam.cs.hackhub.controllers.requests.CreateHackathonRequest;
import it.unicam.cs.hackhub.model.entities.Hackathon;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Builds only the eight attributes Hackathon owns: staff, state and id remain the
 * service's responsibility.
 * <p>
 * The scope is prototype because the builder accumulates state between one setter and the
 * next: as a singleton, two concurrent requests would overwrite each other's fields
 * halfway through the construction.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class HackathonBuilder {

    private String name;
    private String rules;
    private String location;
    private BigDecimal prize;
    private LocalDateTime registrationDeadline;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int maxTeamSize;

    public void setName(String name) {
        this.name = name;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPrize(BigDecimal prize) {
        this.prize = prize;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }

    public void setHackathonInformation(CreateHackathonRequest req) {
        setName(req.getName());
        setRules(req.getRules());
        setLocation(req.getLocation());
        setPrize(req.getPrize());
        setRegistrationDeadline(req.getRegistrationDeadline());
        setStartDate(req.getStartDate());
        setEndDate(req.getEndDate());
        setMaxTeamSize(req.getMaxTeamSize());
    }

    public Hackathon getResult() {
        return new Hackathon(name, rules, location, prize,
                registrationDeadline, startDate, endDate, maxTeamSize);
    }
}
