package com.bervan.cookbook;

import com.bervan.cookbook.service.DietDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cook-book/diet")
public class DashboardRestController {

    private final DietDashboardService dashboardService;

    public DashboardRestController(DietDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDto> dashboard(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "DAY") String groupBy
    ) {
        LocalDate fromDate = from.isBlank() ? LocalDate.now().minusDays(30) : LocalDate.parse(from);
        LocalDate toDate = to.isBlank() ? LocalDate.now() : LocalDate.parse(to);
        DietDashboardService.GroupBy gb;
        try {
            gb = DietDashboardService.GroupBy.valueOf(groupBy.toUpperCase());
        } catch (IllegalArgumentException e) {
            gb = DietDashboardService.GroupBy.DAY;
        }

        DietDashboardService.DietChartData data = dashboardService.getChartData(fromDate, toDate, gb);
        DietDashboardService.MacroBreakdownData macro = dashboardService.getMacroBreakdown(fromDate, toDate);
        DietDashboardService.WeightProjectionData proj = dashboardService.getWeightProjectionData();

        DietChartDataDto chartDto = new DietChartDataDto();
        chartDto.setLabels(data.labels());
        chartDto.setActivityKcal(data.activityKcal());
        chartDto.setConsumedKcal(data.consumedKcal());
        chartDto.setTargetKcal(data.targetKcal());
        chartDto.setEffectiveTdee(data.effectiveTdee());
        chartDto.setDeficit(data.deficit());
        chartDto.setWeight(data.weight());

        MacroBreakdownDto macroDto = new MacroBreakdownDto();
        macroDto.setAvgConsumedProtein(macro.avgConsumedProtein());
        macroDto.setAvgConsumedFat(macro.avgConsumedFat());
        macroDto.setAvgConsumedCarbs(macro.avgConsumedCarbs());
        macroDto.setAvgTargetProtein(macro.avgTargetProtein());
        macroDto.setAvgTargetFat(macro.avgTargetFat());
        macroDto.setAvgTargetCarbs(macro.avgTargetCarbs());
        macroDto.setHasData(macro.hasData());

        WeightProjectionDto projDto = new WeightProjectionDto();
        projDto.setLabels(proj.labels());
        projDto.setActualWeight(proj.actualWeight());
        projDto.setProjectedWeight(proj.projectedWeight());
        projDto.setAvgDailyDeficit(proj.avgDailyDeficit());
        projDto.setWeeklyWeightChange(proj.weeklyWeightChange());

        DashboardDto dashboardDto = new DashboardDto();
        dashboardDto.setChartData(chartDto);
        dashboardDto.setMacroBreakdown(macroDto);
        dashboardDto.setWeightProjection(projDto);

        return ResponseEntity.ok(dashboardDto);
    }
}
