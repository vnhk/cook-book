package com.bervan.cookbook;

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
public class WeightProjectionDto {
    private UUID id;
    private List<String> labels;
    private List<Double> actualWeight;
    private List<Double> projectedWeight;
    private double avgDailyDeficit;
    private double weeklyWeightChange;

}
