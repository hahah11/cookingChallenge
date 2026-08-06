package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateParticipantsRequestRestDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

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

        service.execute(challenge.getId().toString(), organizerId,
                new UpdateParticipantsRequestRestDto().addGuestAccountIds(List.of(guest.toString())));

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

        service.execute(challenge.getId().toString(), organizerId,
                new UpdateParticipantsRequestRestDto().cookBAccountId(newCookB.toString()));

        assertEquals(newCookB, challenge.cookAssignmentFor(DishLabel.B).accountId());
        assertNull(challenge.cookAssignmentFor(DishLabel.A).colorId());
        assertNull(challenge.cookAssignmentFor(DishLabel.B).colorId());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(
                missingId.toString(), organizerId, new UpdateParticipantsRequestRestDto()));
    }

    @Test
    void should_throw_when_accountCannotOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.execute(
                ChallengeId.generate().toString(), organizerId, new UpdateParticipantsRequestRestDto()));
        verify(challengeRepository, never()).findById(any());
    }
}
