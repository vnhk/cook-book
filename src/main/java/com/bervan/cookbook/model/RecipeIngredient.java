package com.bervan.cookbook.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import com.bervan.ieentities.ExcelIEEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class RecipeIngredient extends BervanOwnedBaseEntity<UUID> implements ExcelIEEntity<UUID> {

    @Id
    private UUID id;

    @ManyToOne
    @NotNull
    private Recipe recipe;

    @ManyToOne
    @NotNull
    private Ingredient ingredient;

    private Double quantity;

    @Enumerated(EnumType.STRING)
    private CulinaryUnit unit;

    private Boolean optional;

    @Size(max = 100)
    private String category;

    @Size(max = 300)
    private String originalText;

    private boolean deleted;
    private LocalDateTime modificationDate;

    public RecipeIngredient() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public LocalDateTime getModificationDate() {
        return modificationDate;
    }

    @Override
    public void setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    public Boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public CulinaryUnit getUnit() {
        return unit;
    }

    public void setUnit(CulinaryUnit unit) {
        this.unit = unit;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
}
