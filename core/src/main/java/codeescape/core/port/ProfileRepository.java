package codeescape.core.port;

import codeescape.core.model.PlayerProfile;
import java.util.Optional;

public interface ProfileRepository {
    Optional<PlayerProfile> findByUsername(String username);
    boolean authenticate(String username, String password);
    void saveAccount(String username, String email, String password, PlayerProfile profile);
    void saveProfile(PlayerProfile profile);
    int getLifetimeXp(String username);
    void commitAttemptXp(String username, int earnedXp);
}
