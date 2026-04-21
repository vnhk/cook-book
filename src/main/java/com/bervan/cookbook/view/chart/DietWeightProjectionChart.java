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
public class DietWeightProjectionChart extends Component implements HasSize {

    public DietWeightProjectionChart(List<String> labels, List<Double> actualWeight, List<Double> projectedWeight) {
        setId("dietWeightProjectionChart_" + UUID.randomUUID());
        UI.getCurrent().getPage().executeJs(
                "window.renderDietWeightProjectionChart($0, $1, $2, $3)",
                getElement(),
                DietActivityChart.toJson(labels).get("data"),
                DietActivityChart.toJson(actualWeight).get("data"),
                DietActivityChart.toJson(projectedWeight).get("data")
        );
    }
}
