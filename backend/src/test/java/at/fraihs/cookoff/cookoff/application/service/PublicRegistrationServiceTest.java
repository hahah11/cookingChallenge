package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.RegistrationInvites;
import at.fraihs.cookoff.auth.RegistrationResult;
import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationResultRestDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicRegistrationServiceTest {

    @Mock
    private RegistrationInvites registrationInvites;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private PublicRegistrationService service;

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(), AccountId.generate());
    }

    private static PublicRegistrationRequestRestDto request(String token) {
        return new PublicRegistrationRequestRestDto(token, "Walk", "In", "walkin@example.com");
    }

    @Test
    void should_registerAndJoinChallenge_when_challengeIsStillOpen() {
        Challenge challenge = openChallenge();
        AccountId newAccountId = AccountId.generate();
        when(registrationInvites.register("tok", "Walk", "In", "walkin@example.com"))
                .thenReturn(new RegistrationResult(newAccountId, challenge.getId().value()));
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        PublicRegistrationResultRestDto result = service.execute(request("tok"));

        assertTrue(result.getJoined());
        assertEquals(newAccountId.toString(), result.getAccountId());
        assertTrue(challenge.isGuest(newAccountId));
        verify(challengeRepository).save(challenge);
    }

    @Test
    void should_registerWithoutJoining_when_challengeIsNoLongerOpen() {
        Challenge challenge = openChallenge();
        challenge.reveal(null);
        AccountId newAccountId = AccountId.generate();
        when(registrationInvites.register("tok", "Walk", "In", "walkin@example.com"))
                .thenReturn(new RegistrationResult(newAccountId, challenge.getId().value()));
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        PublicRegistrationResultRestDto result = service.execute(request("tok"));

        assertFalse(result.getJoined());
        assertEquals(newAccountId.toString(), result.getAccountId());
        verify(challengeRepository, never()).save(challenge);
    }

    @Test
    void should_throw_when_emailIsAlreadyRegistered() {
        when(registrationInvites.register("tok", "Walk", "In", "walkin@example.com"))
                .thenThrow(new AccountAlreadyExistsException("walkin@example.com"));

        assertThrows(AccountAlreadyExistsException.class, () -> service.execute(request("tok")));
    }

    @Test
    void should_throw_when_tokenIsInvalidOrExpired() {
        when(registrationInvites.register("bad-tok", "Walk", "In", "walkin@example.com"))
                .thenThrow(new InvalidOrExpiredLinkException());

        assertThrows(InvalidOrExpiredLinkException.class, () -> service.execute(request("bad-tok")));
    }
}
