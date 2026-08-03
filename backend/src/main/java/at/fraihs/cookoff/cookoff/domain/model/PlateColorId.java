package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.shared.tsid.TsidSupport;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record PlateColorId(long value) {

    public static PlateColorId generate() {
        return new PlateColorId(TsidSupport.generate());
    }

    public static PlateColorId fromString(String base32) {
        return new PlateColorId(TsidSupport.fromBase32(base32));
    }

    @Override
    public String toString() {
        return TsidSupport.toBase32(value);
    }
}
