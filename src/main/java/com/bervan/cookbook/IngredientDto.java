package com.bervan.cookbook;

import com.bervan.cookbook.model.Ingredient;
import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class IngredientDto implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String icon;
    private String category;
    private String notes;
    private Double kcalPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbsPer100g;
    private Double fiberPer100g;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Ingredient.class;
    }
}
