package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.shared.tsid.TsidSupport;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record ScoreSubmissionId(long value) {

    public static ScoreSubmissionId generate() {
        return new ScoreSubmissionId(TsidSupport.generate());
    }

    public static ScoreSubmissionId fromString(String base32) {
        return new ScoreSubmissionId(TsidSupport.fromBase32(base32));
    }

    @Override
    public String toString() {
        return TsidSupport.toBase32(value);
    }
}
