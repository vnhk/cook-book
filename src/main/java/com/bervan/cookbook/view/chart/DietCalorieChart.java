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
public class DietCalorieChart extends Component implements HasSize {

    public DietCalorieChart(List<String> labels, List<Double> consumed,
                             List<Double> target, List<Double> effectiveTdee) {
        setId("dietCalorieChart_" + UUID.randomUUID());
        UI.getCurrent().getPage().executeJs(
                "window.renderDietCalorieChart($0, $1, $2, $3, $4)",
                getElement(),
                DietActivityChart.toJson(labels).get("data"),
                DietActivityChart.toJson(consumed).get("data"),
                DietActivityChart.toJson(target).get("data"),
                DietActivityChart.toJson(effectiveTdee).get("data")
        );
    }
}
