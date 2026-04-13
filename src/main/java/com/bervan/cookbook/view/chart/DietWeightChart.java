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
public class DietWeightChart extends Component implements HasSize {

    public DietWeightChart(List<String> labels, List<Double> weight) {
        setId("dietWeightChart_" + UUID.randomUUID());
        UI.getCurrent().getPage().executeJs(
                "window.renderDietWeightChart($0, $1, $2)",
                getElement(),
                DietActivityChart.toJson(labels).get("data"),
                DietActivityChart.toJson(weight).get("data")
        );
    }
}
