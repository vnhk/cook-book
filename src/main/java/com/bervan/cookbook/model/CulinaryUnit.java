package com.bervan.cookbook.model;

public enum CulinaryUnit {
    // Weight
    GRAM("g", BaseUnitType.WEIGHT, 1.0),
    KILOGRAM("kg", BaseUnitType.WEIGHT, 1000.0),
    DEKAGRAM("dag", BaseUnitType.WEIGHT, 10.0),

    // Volume
    MILLILITER("ml", BaseUnitType.VOLUME, 1.0),
    LITER("l", BaseUnitType.VOLUME, 1000.0),
    TEASPOON("łyżeczka", BaseUnitType.VOLUME, 5.0),
    TABLESPOON("łyżka", BaseUnitType.VOLUME, 15.0),
    GLASS("szklanka", BaseUnitType.VOLUME, 250.0),

    // Count
    PIECE("sztuka", BaseUnitType.COUNT, 1.0),
    PINCH("szczypta", BaseUnitType.COUNT, 1.0),
    BUNCH("pęczek", BaseUnitType.COUNT, 1.0),
    CLOVE("ząbek", BaseUnitType.COUNT, 1.0),
    SLICE("plasterek", BaseUnitType.COUNT, 1.0),
    HANDFUL("garść", BaseUnitType.COUNT, 1.0),
    PACK("opakowanie", BaseUnitType.COUNT, 1.0);

    private final String displayName;
    private final BaseUnitType baseType;
    private final double toBaseRate;

    CulinaryUnit(String displayName, BaseUnitType baseType, double toBaseRate) {
        this.displayName = displayName;
        this.baseType = baseType;
        this.toBaseRate = toBaseRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BaseUnitType getBaseType() {
        return baseType;
    }

    public double getToBaseRate() {
        return toBaseRate;
    }

    public String getBaseUnitName() {
        return switch (baseType) {
            case WEIGHT -> "g";
            case VOLUME -> "ml";
            case COUNT -> "szt";
        };
    }

    public enum BaseUnitType {
        WEIGHT, VOLUME, COUNT
    }
}
