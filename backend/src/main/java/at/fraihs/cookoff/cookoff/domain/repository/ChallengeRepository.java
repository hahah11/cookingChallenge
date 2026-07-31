package at.fraihs.cookoff.cookoff.domain.repository;

import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;

import java.util.List;
import java.util.Optional;

public interface ChallengeRepository {

    Optional<Challenge> findById(ChallengeId id);

    List<Challenge> findAll();

    Challenge save(Challenge challenge);
}
