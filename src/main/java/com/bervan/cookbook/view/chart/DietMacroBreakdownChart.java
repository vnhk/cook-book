package com.bervan.cookbook.view.chart;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;

import java.util.List;
import java.util.UUID;

@JsModule("./diet-chart-component.js")
@Tag("canvas")
public class DietMacroBreakdownChart extends Component implements HasSize {

    public DietMacroBreakdownChart(List<Double> consumed, List<Double> targets) {
        setId("dietMacroBreakdownChart_" + UUID.randomUUID());
        UI.getCurrent().getPage().executeJs(
                "window.renderDietMacroBreakdownChart($0, $1, $2)",
                getElement(),
                DietActivityChart.toJson(consumed).get("data"),
                DietActivityChart.toJson(targets).get("data")
        );
    }
}
