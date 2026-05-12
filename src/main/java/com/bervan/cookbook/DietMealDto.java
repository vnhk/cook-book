package com.bervan.cookbook;

import com.bervan.cookbook.model.DietMeal;
import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class DietMealDto implements BaseDTO<UUID> {
    private UUID id;
    private String mealType;
    private String mealTypeName;
    private List<DietMealItemDto> items;
    private double totalKcal;
    private double totalProtein;
    private double totalFat;
    private double totalCarbs;
    private double totalFiber;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return DietMeal.class;
    }
}
