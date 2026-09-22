package at.fraihs.cookoff.auth.infrastructure.passwordreset;

import at.fraihs.cookoff.auth.infrastructure.passwordreset.entity.PasswordResetTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, Long> {

    Optional<PasswordResetTokenJpaEntity> findByToken(String token);

    /**
     * The single-use guarantee. A find-check-save would let two concurrent redeems both read
     * {@code usedAt IS NULL} at READ_COMMITTED; this conditional UPDATE lets exactly one of them
     * match. Expiry is in the same WHERE so no second read can disagree with it.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetTokenJpaEntity t set t.usedAt = :now "
            + "where t.token = :token and t.usedAt is null and t.expiresAt > :now")
    int claim(@Param("token") String token, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PasswordResetTokenJpaEntity t where t.accountId = :accountId")
    void deleteAllByAccountId(@Param("accountId") Long accountId);
}
