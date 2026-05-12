package com.bervan.cookbook;

import com.bervan.cookbook.model.DietDay;
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
public class DietDayDto implements BaseDTO<UUID> {
    private UUID id;
    private String date;
    private Integer targetKcal;
    private Integer estimatedDailyKcal;
    private Integer targetProtein;
    private Integer targetCarbs;
    private Integer targetFat;
    private Integer targetFiber;
    private Integer activityKcal;
    private Integer activityKcalPercent;
    private Double weightKg;
    private String notes;
    private Integer age;
    private String gender;
    private Integer heightCm;
    private String activityLevel;
    private double totalKcal;
    private double totalProtein;
    private double totalFat;
    private double totalCarbs;
    private double totalFiber;
    private List<DietMealDto> meals;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return DietDay.class;
    }
}
