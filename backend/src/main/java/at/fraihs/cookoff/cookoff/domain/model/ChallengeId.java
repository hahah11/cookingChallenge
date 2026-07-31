package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.shared.tsid.TsidSupport;

public record ChallengeId(long value) {

    public static ChallengeId generate() {
        return new ChallengeId(TsidSupport.generate());
    }

    public static ChallengeId fromString(String base32) {
        return new ChallengeId(TsidSupport.fromBase32(base32));
    }

    @Override
    public String toString() {
        return TsidSupport.toBase32(value);
    }
}
