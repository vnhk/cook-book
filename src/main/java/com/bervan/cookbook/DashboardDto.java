package com.bervan.cookbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class DashboardDto {
    private UUID id;
    private DietChartDataDto chartData;
    private MacroBreakdownDto macroBreakdown;
    private WeightProjectionDto weightProjection;
}
