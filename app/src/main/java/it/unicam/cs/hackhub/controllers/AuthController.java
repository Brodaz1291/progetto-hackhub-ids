package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.UserDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.AuthService;
import it.unicam.cs.hackhub.controllers.requests.RegisterRequest;
import it.unicam.cs.hackhub.model.entities.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final DTOMapper dtoMapper;

    public AuthController(AuthService authService, DTOMapper dtoMapper) {
        this.authService = authService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping("/register")
    public UserDTO register(@RequestBody RegisterRequest req) {
        User registeredUser = authService.register(req);
        return dtoMapper.toDTO(registeredUser);
    }
}
