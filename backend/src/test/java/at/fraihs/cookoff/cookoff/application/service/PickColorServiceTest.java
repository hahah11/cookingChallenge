package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.PlateColorRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.shared.web.openapi.model.PickColorRequestRestDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickColorServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private PlateColorRepository plateColorRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @InjectMocks
    private PickColorService service;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();
    private final PlateColor red = PlateColor.create("Red", "#E63946", 1, true);
    private final PlateColor yellow = PlateColor.create("Yellow", "#F4D35E", 2, true);

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(), organizerId);
    }

    @Test
    void should_pickColor_when_callerIsACook() {
        Challenge challenge = openChallenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(plateColorRepository.findAllActiveOrderedBySortOrder()).thenReturn(List.of(red, yellow));

        service.execute(challenge.getId().toString(), cookAId, new PickColorRequestRestDto(red.getId().toString()));

        assertEquals(red.getId(), challenge.cookAssignmentFor(DishLabel.A).colorId());
        assertEquals(yellow.getId(), challenge.cookAssignmentFor(DishLabel.B).colorId());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(
                missingId.toString(), cookAId, new PickColorRequestRestDto(red.getId().toString())));
    }

    @Test
    void should_throw_when_callerIsNotACook() {
        Challenge challenge = openChallenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        AccountId stranger = AccountId.generate();

        assertThrows(NotAParticipantException.class, () -> service.execute(
                challenge.getId().toString(), stranger, new PickColorRequestRestDto(red.getId().toString())));
    }

    @Test
    void should_throw_when_colorIdIsNotAvailable() {
        Challenge challenge = openChallenge();
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(plateColorRepository.findAllActiveOrderedBySortOrder()).thenReturn(List.of(red, yellow));

        assertThrows(IllegalArgumentException.class, () -> service.execute(challenge.getId().toString(), cookAId,
                new PickColorRequestRestDto(PlateColor.create("Blue", "#0000FF", 3, true).getId().toString())));
    }
}
