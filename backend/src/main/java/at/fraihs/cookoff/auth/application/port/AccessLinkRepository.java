package at.fraihs.cookoff.auth.application.port;

import java.util.Optional;

public interface AccessLinkRepository {

    AccessLink save(AccessLink accessLink);

    Optional<AccessLink> findByToken(String token);
}
