package com.bervan.cookbook.view;

import com.bervan.common.view.AbstractPageView;
import com.bervan.cookbook.service.DietDashboardService;
import com.bervan.cookbook.service.DietDashboardService.DietChartData;
import com.bervan.cookbook.service.DietDashboardService.GroupBy;
import com.bervan.cookbook.view.chart.DietActivityChart;
import com.bervan.cookbook.view.chart.DietCalorieChart;
import com.bervan.cookbook.view.chart.DietCumulativeDeficitChart;
import com.bervan.cookbook.view.chart.DietDeficitChart;
import com.bervan.cookbook.view.chart.DietMacroBreakdownChart;
import com.bervan.cookbook.view.chart.DietWeightChart;
import com.bervan.cookbook.view.chart.DietWeightProjectionChart;

import java.util.ArrayList;
import java.util.List;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;

import java.time.LocalDate;

public abstract class AbstractDietDashboardView extends AbstractPageView {

    public static final String ROUTE_NAME = "/cook-book/diet-dashboard";

    private final DietDashboardService dashboardService;

    private GroupBy groupBy = GroupBy.DAY;
    private LocalDate from = LocalDate.now().minusDays(30);
    private LocalDate to = LocalDate.now();

    private final Div chartsContainer = new Div();

    public AbstractDietDashboardView(DietDashboardService dashboardService) {
        this.dashboardService = dashboardService;

        add(new CookBookPageLayout(ROUTE_NAME));

        add(buildControls());
        add(buildFormulaInfo());

        chartsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, 1fr)")
                .set("gap", "1.5rem")
                .set("width", "100%");
        add(chartsContainer);

        refresh();
    }

    private HorizontalLayout buildControls() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setAlignItems(HorizontalLayout.Alignment.END);
        bar.setSpacing(true);
        bar.getStyle().set("flex-wrap", "wrap").set("gap", "0.5rem");

        // Period buttons
        Button btnDay = new Button("Day", e -> { groupBy = GroupBy.DAY; refresh(); });
        Button btnWeek = new Button("Week", e -> { groupBy = GroupBy.WEEK; refresh(); });
        Button btnMonth = new Button("Month", e -> { groupBy = GroupBy.MONTH; refresh(); });

        btnDay.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btnWeek.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btnMonth.addThemeVariants(ButtonVariant.LUMO_SMALL);

        // Date pickers
        DatePicker fromPicker = new DatePicker("From");
        fromPicker.setValue(from);
        fromPicker.addValueChangeListener(e -> from = e.getValue());

        DatePicker toPicker = new DatePicker("To");
        toPicker.setValue(to);
        toPicker.addValueChangeListener(e -> to = e.getValue());

        Button apply = new Button("Apply", e -> refresh());
        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        bar.add(new Span("Group by:"), btnDay, btnWeek, btnMonth, fromPicker, toPicker, apply);
        return bar;
    }

    private Div buildFormulaInfo() {
        Div info = new Div();
        info.getStyle()
                .set("font-size", "0.75rem")
                .set("opacity", "0.7")
                .set("padding", "0.25rem 0");

        Span text = new Span("Net Deficit = TDEE − Consumed  |  Green bars = deficit (weight loss), red = surplus");
        info.add(text);
        return info;
    }

    private void refresh() {
        if (from == null || to == null || from.isAfter(to)) return;

        DietChartData data = dashboardService.getChartData(from, to, groupBy);
        chartsContainer.removeAll();

        if (!data.labels().isEmpty()) {
            chartsContainer.add(chartCard("Activity Calories Burned",
                    new DietActivityChart(data.labels(), data.activityKcal())));

            chartsContainer.add(chartCard("Net Deficit (green = deficit, red = surplus)",
                    new DietDeficitChart(data.labels(), data.deficit())));

            chartsContainer.add(chartCard("Calorie Intake vs Target vs TDEE",
                    new DietCalorieChart(data.labels(), data.consumedKcal(),
                            data.targetKcal(), data.effectiveTdee())));

            // Cumulative deficit
            List<Double> cumulative = new ArrayList<>();
            double running = 0;
            for (Double d : data.deficit()) {
                running += d != null ? d : 0;
                cumulative.add(Math.round(running * 10.0) / 10.0);
            }
            chartsContainer.add(chartCard("Cumulative Deficit (tooltip: ≈ kg fat burned)",
                    new DietCumulativeDeficitChart(data.labels(), cumulative)));

            // Macro breakdown
            DietDashboardService.MacroBreakdownData macro = dashboardService.getMacroBreakdown(from, to);
            if (macro.hasData()) {
                List<Double> consumed = List.of(macro.avgConsumedProtein(), macro.avgConsumedFat(), macro.avgConsumedCarbs());
                List<Double> targets  = List.of(macro.avgTargetProtein(),  macro.avgTargetFat(),  macro.avgTargetCarbs());
                chartsContainer.add(chartCard("Macro Breakdown — avg/day (Protein / Fat / Carbs)",
                        new DietMacroBreakdownChart(consumed, targets)));
            }

            boolean hasWeight = data.weight().stream().anyMatch(w -> w != null);
            if (hasWeight) {
                chartsContainer.add(chartCard("Weight (kg)",
                        new DietWeightChart(data.labels(), data.weight())));
            }
        } else {
            Div empty = new Div(new Span("No data for selected range."));
            empty.getStyle().set("grid-column", "1 / -1").set("text-align", "center").set("padding", "2rem");
            chartsContainer.add(empty);
        }

        DietDashboardService.WeightProjectionData proj = dashboardService.getWeightProjectionData();
        boolean hasProjection = proj.projectedWeight().stream().anyMatch(w -> w != null);
        if (hasProjection) {
            String subtitle = String.format("avg deficit %.0f kcal/day → %s%.2f kg/week",
                    proj.avgDailyDeficit(),
                    proj.weeklyWeightChange() >= 0 ? "-" : "+",
                    Math.abs(proj.weeklyWeightChange()));
            chartsContainer.add(chartCard("Weight Projection — 13 weeks  (" + subtitle + ")",
                    new DietWeightProjectionChart(proj.labels(), proj.actualWeight(), proj.projectedWeight())));
        }
    }

    private Div chartCard(String title, com.vaadin.flow.component.Component chart) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("glass-card");
        card.getStyle()
                .set("padding", "1rem")
                .set("border-radius", "0.75rem");

        H4 h = new H4(title);
        h.getStyle().set("margin", "0 0 0.5rem 0").set("font-size", "0.9rem");

        chart.getElement().getStyle().set("width", "100%").set("max-height", "280px");

        card.add(h, chart);

        Div wrapper = new Div(card);
        return wrapper;
    }
}
