package at.fraihs.cookoff.auth.domain.model;

import at.fraihs.cookoff.shared.tsid.TsidSupport;

public record AccountId(long value) {

    public static AccountId generate() {
        return new AccountId(TsidSupport.generate());
    }

    public static AccountId fromString(String base32) {
        return new AccountId(TsidSupport.fromBase32(base32));
    }

    @Override
    public String toString() {
        return TsidSupport.toBase32(value);
    }
}
