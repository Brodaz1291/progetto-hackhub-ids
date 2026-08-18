package it.unicam.cs.hackhub.application.services;

import it.unicam.cs.hackhub.application.exceptions.AuthenticationException;
import it.unicam.cs.hackhub.application.exceptions.ConflictException;
import it.unicam.cs.hackhub.controllers.requests.RegisterRequest;
import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
            throw new ConflictException("Username already in use: " + username);
        }
        User newUser = new User(username, req.getPassword());
        return userRepository.save(newUser);
    }

    private boolean checkUsernameAvailable(String username) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        return existingUser.isEmpty();
    }

    /**
     * Authenticates a user with the credentials provided at login.
     *
     * The error message is the same whether the username does not exist or the password is
     * wrong: telling the two cases apart would reveal which usernames are registered.
     */
    public User login(String username, String password) {
        Optional<User> foundUser = userRepository.findByUsername(username);
        if (foundUser.isEmpty()) {
            throw new AuthenticationException("Invalid credentials");
        }
        User user = foundUser.get();
        if (!checkCredentials(user, password)) {
            throw new AuthenticationException("Invalid credentials");
        }
        return user;
    }

    /**
     * Closes the session of a user.
     *
     * Session handling is delegated to the presentation layer, which at the moment does not
     * keep any session state: the identity travels as an explicit parameter, so there is
     * nothing to invalidate and the method only records the event. This is the place to
     * extend once tokens or a real security context are introduced.
     */
    public void logout(Long userId) {
        log.info("User {} closed the session", userId);
    }

    /**
     * NOTE: the comparison is on the clear text password, consistently with register and
     * DevDataSeeder. It must become an encoder check once one is introduced.
     */
    private boolean checkCredentials(User user, String password) {
        return user.getPassword().equals(password);
    }
}
