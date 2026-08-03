package at.fraihs.cookoff.cookoff.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
public class PlateColor {

    @Identity
    private final PlateColorId id;
    private final String name;
    private final String hexCode;
    private final int sortOrder;
    private final boolean active;

    private PlateColor(PlateColorId id, String name, String hexCode, int sortOrder, boolean active) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("PlateColor name must not be blank");
        }
        if (hexCode == null || hexCode.isBlank()) {
            throw new IllegalArgumentException("PlateColor hexCode must not be blank");
        }
        this.id = id;
        this.name = name;
        this.hexCode = hexCode;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public static PlateColor create(String name, String hexCode, int sortOrder, boolean active) {
        return new PlateColor(PlateColorId.generate(), name, hexCode, sortOrder, active);
    }

    public static PlateColor reconstitute(PlateColorId id, String name, String hexCode, int sortOrder,
                                           boolean active) {
        return new PlateColor(id, name, hexCode, sortOrder, active);
    }

    public PlateColorId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHexCode() {
        return hexCode;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
