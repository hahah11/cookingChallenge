package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * The outcome recorded by a challenge's most recent reveal; {@code null} winnerAccountId
 * means that reveal was a draw. A {@code null} reference to this type (as opposed to an
 * instance with a null winner) distinguishes "never revealed" from "revealed, then drew".
 */
@ValueObject
public record RevealResult(AccountId winnerAccountId) {
}
