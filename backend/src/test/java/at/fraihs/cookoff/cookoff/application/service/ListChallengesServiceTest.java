package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.shared.web.dto.PagedResult;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListChallengesServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @Mock
    private AccountLookup accountLookup;

    @InjectMocks
    private ListChallengesService service;

    private final AccountId organizerId = AccountId.generate();
    private final AccountId adminId = AccountId.generate();
    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();

    private Challenge challenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(), organizerId);
    }

    private void stubCookLookups() {
        when(accountLookup.getById(cookAId)).thenReturn(new AccountSummary(cookAId, new Email("a@x.com"), "Cook A", "Cook"));
        when(accountLookup.getById(cookBId)).thenReturn(new AccountSummary(cookBId, new Email("b@x.com"), "Cook B", "Cook"));
    }

    @Test
    void should_listOnlyChallengesCreatedByTheRequester_when_requesterIsNotAdmin() {
        Challenge challenge = challenge();
        when(accountLookup.isAdmin(organizerId)).thenReturn(false);
        when(challengeRepository.findAllByCreatedBy(organizerId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(challenge)));
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId())).thenReturn(List.of());
        stubCookLookups();

        PagedResult<?> result = service.execute(organizerId, 0, 20);

        assertEquals(1L, result.pagination().getTotalElements());
        verify(challengeRepository, never()).findAll(any());
    }

    @Test
    void should_listEveryChallenge_when_requesterIsAdmin() {
        Challenge challenge = challenge();
        when(accountLookup.isAdmin(adminId)).thenReturn(true);
        when(challengeRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(challenge)));
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId())).thenReturn(List.of());
        stubCookLookups();

        PagedResult<?> result = service.execute(adminId, 0, 20);

        assertEquals(1L, result.pagination().getTotalElements());
        verify(challengeRepository, never()).findAllByCreatedBy(any(), any());
    }
}
