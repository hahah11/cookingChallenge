package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.shared.tsid.TsidSupport;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record CookRivalryId(long value) {

    public static CookRivalryId generate() {
        return new CookRivalryId(TsidSupport.generate());
    }

    public static CookRivalryId fromString(String base32) {
        return new CookRivalryId(TsidSupport.fromBase32(base32));
    }

    @Override
    public String toString() {
        return TsidSupport.toBase32(value);
    }
}
