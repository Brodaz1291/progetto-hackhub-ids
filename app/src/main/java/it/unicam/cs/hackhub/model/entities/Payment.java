package it.unicam.cs.hackhub.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    private LocalDateTime date;

    private String externalId;

    @OneToOne
    @JoinColumn(name = "registration_id")
    @JsonIgnore
    private Registration registration;

    public Payment(BigDecimal amount, LocalDateTime date, String externalId) {
        this.amount = amount;
        this.date = date;
        this.externalId = externalId;
    }
}
