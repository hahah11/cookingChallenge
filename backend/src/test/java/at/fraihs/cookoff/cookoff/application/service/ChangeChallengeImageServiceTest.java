package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChangeChallengeImageCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ImageStoragePort;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeChallengeImageServiceTest {

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ImageStoragePort imageStoragePort;

    @InjectMocks
    private ChangeChallengeImageService service;

    private final AccountId organizerId = AccountId.generate();
    private final byte[] imageBytes = {1, 2, 3};

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(), organizerId);
    }

    @Test
    void should_storeAndSetImage_when_challengeHasNoExistingImage() {
        Challenge challenge = openChallenge();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(imageStoragePort.store(imageBytes, "image/png")).thenReturn("new-ref");

        service.execute(new ChangeChallengeImageCommand(challenge.getId().toString(), organizerId.toString(), "image/png"),
                imageBytes);

        assertEquals("new-ref", challenge.getImageRef());
        verify(challengeRepository).save(challenge);
        verify(imageStoragePort, never()).delete(any());
    }

    @Test
    void should_deleteOldImageAfterSaving_when_replacingExistingImage() {
        Challenge challenge = openChallenge();
        challenge.changeImage("old-ref");
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(imageStoragePort.store(imageBytes, "image/png")).thenReturn("new-ref");

        service.execute(new ChangeChallengeImageCommand(challenge.getId().toString(), organizerId.toString(), "image/png"),
                imageBytes);

        assertEquals("new-ref", challenge.getImageRef());
        var inOrder = org.mockito.Mockito.inOrder(challengeRepository, imageStoragePort);
        inOrder.verify(challengeRepository).save(challenge);
        inOrder.verify(imageStoragePort).delete("old-ref");
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(
                new ChangeChallengeImageCommand(missingId.toString(), organizerId.toString(), "image/png"), imageBytes));
    }

    @Test
    void should_throw_when_accountCannotOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.execute(
                new ChangeChallengeImageCommand(ChallengeId.generate().toString(), organizerId.toString(), "image/png"),
                imageBytes));
        verify(challengeRepository, never()).findById(any());
    }
}
