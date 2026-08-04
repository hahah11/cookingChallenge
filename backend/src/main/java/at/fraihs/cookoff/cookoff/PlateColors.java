package at.fraihs.cookoff.cookoff;

import java.util.List;

/**
 * Public contract for other modules that need the active plate-color palette. Keeps
 * {@code PlateColor}/{@code PlateColorRepository} internal to the cookoff module (see
 * docs/backend/02-ddd-modulith.md's Module Contracts pattern, same as {@code auth.AccountLookup}).
 */
public interface PlateColors {

    /** Active colors, ordered by sortOrder - the first 2 are every challenge's cook-pick pair. */
    List<PlateColorSummary> listActive();
}
