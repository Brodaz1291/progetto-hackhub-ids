package it.unicam.cs.hackhub.controllers;

import it.unicam.cs.hackhub.application.dtos.UserDTO;
import it.unicam.cs.hackhub.application.mappers.DTOMapper;
import it.unicam.cs.hackhub.application.services.AuthService;
import it.unicam.cs.hackhub.controllers.requests.RegisterRequest;
import it.unicam.cs.hackhub.model.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO register(@RequestBody RegisterRequest req) {
        User registeredUser = authService.register(req);
        return dtoMapper.toDTO(registeredUser);
    }

    /**
     * Authenticates a registered user.
     *
     * NOTE: the model prescribes two simple parameters, so the credentials are read from the
     * query string: they end up in the server logs and in the browser history. It is
     * acceptable here, where passwords are stored in clear text anyway, but a real
     * application would send them in the request body over HTTPS.
     */
    @PostMapping("/login")
    public UserDTO login(@RequestParam String username, @RequestParam String password) {
        User authenticatedUser = authService.login(username, password);
        return dtoMapper.toDTO(authenticatedUser);
    }

    @PostMapping("/logout")
    public void logout(@RequestParam Long userId) {
        authService.logout(userId);
    }
}
