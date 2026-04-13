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
public class DietDeficitChart extends Component implements HasSize {

    public DietDeficitChart(List<String> labels, List<Double> deficit) {
        setId("dietDeficitChart_" + UUID.randomUUID());
        UI.getCurrent().getPage().executeJs(
                "window.renderDietDeficitChart($0, $1, $2)",
                getElement(),
                DietActivityChart.toJson(labels).get("data"),
                DietActivityChart.toJson(deficit).get("data")
        );
    }
}
