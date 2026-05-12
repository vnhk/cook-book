package com.bervan.cookbook;

import com.bervan.cookbook.model.DietMealItem;
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
public class DietMealItemDto implements BaseDTO<UUID> {
    private UUID id;
    private String displayName;
    private String description;
    private UUID ingredientId;
    private String ingredientName;
    private Double amountGrams;
    private double kcal;
    private double protein;
    private double fat;
    private double carbs;
    private double fiber;
    private boolean quickEntry;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return DietMealItem.class;
    }
}
