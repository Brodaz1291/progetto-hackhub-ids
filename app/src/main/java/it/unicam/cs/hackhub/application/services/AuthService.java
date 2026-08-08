package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.controllers.requests.RegisterRequest;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user on the platform.
     *
     * NOTE: the password is stored in clear text because Spring Security is not configured
     * yet, consistently with DevDataSeeder. Once a password encoder is introduced, both
     * classes must be updated together.
     */
    public User register(RegisterRequest req) {
        String username = req.getUsername();
        if (!checkUsernameAvailable(username)) {
            throw new IllegalArgumentException("Username already in use: " + username);
        }
        User newUser = new User(username, req.getPassword());
        return userRepository.save(newUser);
    }

    private boolean checkUsernameAvailable(String username) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        return existingUser.isEmpty();
    }
}
