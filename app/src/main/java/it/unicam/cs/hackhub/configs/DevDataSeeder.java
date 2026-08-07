package it.unicam.cs.hackhub.configs;

import it.unicam.cs.hackhub.model.entities.User;
import it.unicam.cs.hackhub.model.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Preloads a few development users at startup, so login and hackathon creation are possible
 * on the in-memory database (ddl-auto=create-drop empties it at every restart, and no use
 * case creates users other than "register to the platform").
 *
 * NOTE: passwords are stored in clear text because Spring Security is not configured yet.
 * Once a password encoder is introduced, this class must be updated to encode them.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final List<String> DEV_USERNAMES = List.of(
            "organizer1", "judge1", "mentor1", "mentor2", "member1", "member2", "member3");

    private final UserRepository userRepository;

    public DevDataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        List<User> users = DEV_USERNAMES.stream()
                .map(username -> new User(username, "password"))
                .toList();
        userRepository.saveAll(users);
        log.info("DevDataSeeder: created {} development users", users.size());
    }
}
