package at.fraihs.cookoff.auth.application.port;

import at.fraihs.cookoff.auth.application.dto.AccessLink;

import java.util.Optional;

public interface AccessLinkRepository {

    AccessLink save(AccessLink accessLink);

    Optional<AccessLink> findByToken(String token);
}
