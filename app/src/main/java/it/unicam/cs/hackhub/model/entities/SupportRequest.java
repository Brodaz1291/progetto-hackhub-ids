package it.unicam.cs.hackhub.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unicam.cs.hackhub.model.enums.SupportRequestState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Enumerated(EnumType.STRING)
    private SupportRequestState status;

    @ManyToOne
    @JoinColumn(name = "registration_id")
    private Registration registration;

    @OneToOne(mappedBy = "supportRequest")
    @JsonIgnore
    private Call call;

    public SupportRequest(Registration registration, String description, SupportRequestState status) {
        this.registration = registration;
        this.description = description;
        this.status = status;
    }
}
