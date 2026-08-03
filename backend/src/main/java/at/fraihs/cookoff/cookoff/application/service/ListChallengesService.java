package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Organizer-facing history list — always includes the cook↔label mapping. */
@Service
@RequiredArgsConstructor
public class ListChallengesService {

    private final ChallengeRepository challengeRepository;

    @Transactional(readOnly = true)
    public List<ChallengeView> execute() {
        return challengeRepository.findAll().stream().map(ChallengeView::from).toList();
    }
}
