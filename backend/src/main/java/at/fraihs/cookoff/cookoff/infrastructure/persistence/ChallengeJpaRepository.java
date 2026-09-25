package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ChallengeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ChallengeJpaRepository extends JpaRepository<ChallengeJpaEntity, Long> {

    Optional<ChallengeJpaEntity> findByIdAndStatusNot(Long id, ChallengeStatus status);

    Page<ChallengeJpaEntity> findAllByStatusNotOrderByChallengeDateDescIdDesc(ChallengeStatus status, Pageable pageable);

    Page<ChallengeJpaEntity> findAllByCreatedByAccountIdAndStatusNotOrderByChallengeDateDescIdDesc(
            Long createdByAccountId, ChallengeStatus status, Pageable pageable);

    @Query("SELECT DISTINCT c FROM ChallengeJpaEntity c LEFT JOIN c.guestAccountIds g "
            + "WHERE (c.cookAAccountId = :accountId OR c.cookBAccountId = :accountId OR g = :accountId) "
            + "AND c.status <> :excluded ORDER BY c.challengeDate DESC, c.id DESC")
    List<ChallengeJpaEntity> findByParticipant(
            @Param("accountId") Long accountId, @Param("excluded") ChallengeStatus excluded);

    @Query("SELECT c FROM ChallengeJpaEntity c WHERE "
            + "((c.cookAAccountId = :first AND c.cookBAccountId = :second) "
            + "OR (c.cookAAccountId = :second AND c.cookBAccountId = :first)) "
            + "AND c.status <> :excluded ORDER BY c.challengeDate DESC, c.id DESC")
    List<ChallengeJpaEntity> findByCookPair(
            @Param("first") Long firstAccountId, @Param("second") Long secondAccountId,
            @Param("excluded") ChallengeStatus excluded);
}
