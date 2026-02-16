package com.bervan.cookbook.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import com.bervan.ieentities.ExcelIEEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ShoppingCartItem extends BervanOwnedBaseEntity<UUID> implements ExcelIEEntity<UUID> {

    @Id
    private UUID id;

    @ManyToOne
    @NotNull
    private ShoppingCart cart;

    @ManyToOne
    @NotNull
    private Ingredient ingredient;

    private Double quantity;

    @Enumerated(EnumType.STRING)
    private CulinaryUnit unit;

    private Boolean purchased;

    @ManyToOne
    private Recipe sourceRecipe;

    private boolean deleted;
    private LocalDateTime modificationDate;

    public ShoppingCartItem() {
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

    public ShoppingCart getCart() {
        return cart;
    }

    public void setCart(ShoppingCart cart) {
        this.cart = cart;
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

    public Boolean getPurchased() {
        return purchased;
    }

    public void setPurchased(Boolean purchased) {
        this.purchased = purchased;
    }

    public Recipe getSourceRecipe() {
        return sourceRecipe;
    }

    public void setSourceRecipe(Recipe sourceRecipe) {
        this.sourceRecipe = sourceRecipe;
    }
}
