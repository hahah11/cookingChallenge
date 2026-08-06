package at.fraihs.cookoff.auth.application.port;

import at.fraihs.cookoff.auth.application.dto.RegistrationInvite;

import java.util.Optional;

public interface RegistrationInviteRepository {

    RegistrationInvite save(RegistrationInvite registrationInvite);

    Optional<RegistrationInvite> findByToken(String token);
}
