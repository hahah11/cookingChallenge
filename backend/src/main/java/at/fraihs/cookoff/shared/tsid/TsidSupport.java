package at.fraihs.cookoff.shared.tsid;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;

/**
 * Shared TSID generation/encoding used by every aggregate root ID.
 * See docs/backend/03-code-style.md#id-generation-tsid.
 */
public final class TsidSupport {

    private TsidSupport() {
    }

    public static long generate() {
        return TsidCreator.getTsid().toLong();
    }

    public static String toBase32(long value) {
        return Tsid.from(value).toString();
    }

    public static long fromBase32(String base32) {
        return Tsid.from(base32).toLong();
    }
}
