package com.bervan.cookbook.service;

import com.bervan.cookbook.model.CulinaryUnit;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UnitConversionEngine {

    private static final Map<String, CulinaryUnit> UNIT_ALIASES = new HashMap<>();

    static {
        // Weight
        addAliases(CulinaryUnit.GRAM, "g", "gram", "gramy", "gramow", "gramów");
        addAliases(CulinaryUnit.KILOGRAM, "kg", "kilogram", "kilogramy", "kilogramów");
        addAliases(CulinaryUnit.DEKAGRAM, "dag", "dkg", "dekagram", "dekagramy", "dekagramów");

        // Volume
        addAliases(CulinaryUnit.MILLILITER, "ml", "mililitr", "mililitry", "mililitrów");
        addAliases(CulinaryUnit.LITER, "l", "litr", "litry", "litrów");
        addAliases(CulinaryUnit.TEASPOON, "łyżeczka", "łyżeczki", "łyżeczek", "lyzeczka", "lyzeczki");
        addAliases(CulinaryUnit.TABLESPOON, "łyżka", "łyżki", "łyżek", "lyzka", "lyzki");
        addAliases(CulinaryUnit.GLASS, "szklanka", "szklanki", "szklanek");

        // Count
        addAliases(CulinaryUnit.PIECE, "szt", "szt.", "sztuka", "sztuki", "sztuk");
        addAliases(CulinaryUnit.PINCH, "szczypta", "szczypty", "szczypt");
        addAliases(CulinaryUnit.BUNCH, "pęczek", "pęczki", "pęczków", "peczek", "peczki");
        addAliases(CulinaryUnit.CLOVE, "ząbek", "ząbki", "ząbków", "zabek", "zabki");
        addAliases(CulinaryUnit.SLICE, "plasterek", "plasterki", "plasterków");
        addAliases(CulinaryUnit.HANDFUL, "garść", "garście", "garści", "garsc", "garscie");
        addAliases(CulinaryUnit.PACK, "opakowanie", "opakowania", "opakowań");
    }

    private static void addAliases(CulinaryUnit unit, String... aliases) {
        for (String alias : aliases) {
            UNIT_ALIASES.put(alias.toLowerCase(), unit);
        }
    }

    public Double convert(double quantity, CulinaryUnit from, CulinaryUnit to) {
        if (!areCompatible(from, to)) {
            return null;
        }
        double baseAmount = quantity * from.getToBaseRate();
        return baseAmount / to.getToBaseRate();
    }

    public boolean areCompatible(CulinaryUnit a, CulinaryUnit b) {
        return a.getBaseType() == b.getBaseType();
    }

    public CulinaryUnit parseUnit(String text) {
        if (text == null || text.isBlank()) {
            return CulinaryUnit.PIECE;
        }
        String normalized = text.trim().toLowerCase();
        CulinaryUnit unit = UNIT_ALIASES.get(normalized);
        return unit != null ? unit : CulinaryUnit.PIECE;
    }

    public double normalizeToBase(double quantity, CulinaryUnit unit) {
        return quantity * unit.getToBaseRate();
    }

    public String formatQuantity(double quantity, CulinaryUnit unit) {
        double baseAmount = normalizeToBase(quantity, unit);

        if (unit.getBaseType() == CulinaryUnit.BaseUnitType.WEIGHT) {
            if (baseAmount >= 1000) {
                return formatNumber(baseAmount / 1000) + " kg";
            }
            return formatNumber(baseAmount) + " g";
        } else if (unit.getBaseType() == CulinaryUnit.BaseUnitType.VOLUME) {
            if (baseAmount >= 1000) {
                return formatNumber(baseAmount / 1000) + " l";
            }
            return formatNumber(baseAmount) + " ml";
        }
        return formatNumber(quantity) + " " + unit.getDisplayName();
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }
}
