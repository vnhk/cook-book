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
public class DietChartDataDto {
    private UUID id;
    private List<String> labels;
    private List<Double> activityKcal;
    private List<Double> consumedKcal;
    private List<Double> targetKcal;
    private List<Double> effectiveTdee;
    private List<Double> deficit;
    private List<Double> weight;
}
