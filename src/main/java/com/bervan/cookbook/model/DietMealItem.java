package com.bervan.cookbook.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class DietMealItem extends BervanOwnedBaseEntity<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id")
    private DietMeal meal;

    private String description;

    // Quick entry fields
    private Double kcal;
    private Double protein;
    private Double fat;
    private Double carbs;
    private Double fiber;

    // Ingredient-based fields
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
    private Double amountGrams;

    private boolean deleted;
    private LocalDateTime modificationDate;

    public DietMealItem() {}

    public double getEffectiveKcal() {
        if (ingredient != null && ingredient.hasMacros() && amountGrams != null) {
            return ingredient.getKcalPer100g() * amountGrams / 100.0;
        }
        return kcal != null ? kcal : 0.0;
    }

    public double getEffectiveProtein() {
        if (ingredient != null && ingredient.hasMacros() && amountGrams != null) {
            return ingredient.getProteinPer100g() != null ? ingredient.getProteinPer100g() * amountGrams / 100.0 : 0.0;
        }
        return protein != null ? protein : 0.0;
    }

    public double getEffectiveFat() {
        if (ingredient != null && ingredient.hasMacros() && amountGrams != null) {
            return ingredient.getFatPer100g() != null ? ingredient.getFatPer100g() * amountGrams / 100.0 : 0.0;
        }
        return fat != null ? fat : 0.0;
    }

    public double getEffectiveCarbs() {
        if (ingredient != null && ingredient.hasMacros() && amountGrams != null) {
            return ingredient.getCarbsPer100g() != null ? ingredient.getCarbsPer100g() * amountGrams / 100.0 : 0.0;
        }
        return carbs != null ? carbs : 0.0;
    }

    public double getEffectiveFiber() {
        if (ingredient != null && ingredient.hasMacros() && amountGrams != null) {
            return ingredient.getFiberPer100g() != null ? ingredient.getFiberPer100g() * amountGrams / 100.0 : 0.0;
        }
        return fiber != null ? fiber : 0.0;
    }

    public String getDisplayName() {
        if (ingredient != null) {
            return ingredient.getName() + (amountGrams != null ? " (" + amountGrams.intValue() + "g)" : "");
        }
        return description != null ? description : "";
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public void setId(UUID id) { this.id = id; }

    @Override
    public LocalDateTime getModificationDate() { return modificationDate; }

    @Override
    public void setModificationDate(LocalDateTime modificationDate) { this.modificationDate = modificationDate; }

    @Override
    public Boolean isDeleted() { return deleted; }

    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public DietMeal getMeal() { return meal; }
    public void setMeal(DietMeal meal) { this.meal = meal; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getKcal() { return kcal; }
    public void setKcal(Double kcal) { this.kcal = kcal; }

    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }

    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }

    public Double getCarbs() { return carbs; }
    public void setCarbs(Double carbs) { this.carbs = carbs; }

    public Double getFiber() { return fiber; }
    public void setFiber(Double fiber) { this.fiber = fiber; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public Double getAmountGrams() { return amountGrams; }
    public void setAmountGrams(Double amountGrams) { this.amountGrams = amountGrams; }
}
