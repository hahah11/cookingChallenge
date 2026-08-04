package at.fraihs.cookoff.cookoff;

/** Cross-module view of a PlateColor - id pre-formatted, no dependency on the internal typed PlateColorId. */
public record PlateColorSummary(String id, String name, String hexCode, int sortOrder) {
}
