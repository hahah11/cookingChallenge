package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.EditChallengeParticipantsCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditChallengeParticipantsServiceTest {

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private EditChallengeParticipantsService service;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(), organizerId);
    }

    @Test
    void should_addGuest_when_organizerEditsParticipants() {
        Challenge challenge = openChallenge();
        AccountId guest = AccountId.generate();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        service.execute(new EditChallengeParticipantsCommand(
                challenge.getId().toString(), organizerId.toString(), null, null,
                List.of(guest.toString()), List.of()));

        assertTrue(challenge.isGuest(guest));
        verify(challengeRepository).save(challenge);
    }

    @Test
    void should_reassignCookAndClearColors_when_newCookProvided() {
        Challenge challenge = openChallenge();
        challenge.pickColor(cookAId, PlateColorId.generate(), PlateColorId.generate());
        AccountId newCookB = AccountId.generate();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        service.execute(new EditChallengeParticipantsCommand(
                challenge.getId().toString(), organizerId.toString(), null, newCookB.toString(),
                List.of(), List.of()));

        assertEquals(newCookB, challenge.cookAssignmentFor(DishLabel.B).accountId());
        assertEquals(null, challenge.cookAssignmentFor(DishLabel.A).colorId());
        assertEquals(null, challenge.cookAssignmentFor(DishLabel.B).colorId());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(
                new EditChallengeParticipantsCommand(missingId.toString(), organizerId.toString(),
                        null, null, List.of(), List.of())));
    }

    @Test
    void should_throw_when_accountCannotOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.execute(
                new EditChallengeParticipantsCommand(ChallengeId.generate().toString(), organizerId.toString(),
                        null, null, List.of(), List.of())));
        verify(challengeRepository, never()).findById(any());
    }
}
