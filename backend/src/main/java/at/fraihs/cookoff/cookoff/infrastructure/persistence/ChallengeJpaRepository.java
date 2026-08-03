package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ChallengeJpaRepository extends JpaRepository<ChallengeJpaEntity, Long> {

    @Query("SELECT DISTINCT c FROM ChallengeJpaEntity c LEFT JOIN c.guestAccountIds g "
            + "WHERE c.status = :status AND (c.cookAAccountId = :accountId OR c.cookBAccountId = :accountId OR g = :accountId)")
    List<ChallengeJpaEntity> findByStatusAndParticipant(@Param("status") ChallengeStatus status,
                                                         @Param("accountId") Long accountId);

    @Query("SELECT c FROM ChallengeJpaEntity c WHERE "
            + "(c.cookAAccountId = :first AND c.cookBAccountId = :second) "
            + "OR (c.cookAAccountId = :second AND c.cookBAccountId = :first)")
    List<ChallengeJpaEntity> findByCookPair(@Param("first") Long firstAccountId, @Param("second") Long secondAccountId);
}
