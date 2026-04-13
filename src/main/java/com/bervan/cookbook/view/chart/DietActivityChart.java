package com.bervan.cookbook.view.chart;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonObject;

import java.util.List;
import java.util.UUID;

@JsModule("./diet-chart-component.js")
@Tag("canvas")
public class DietActivityChart extends Component implements HasSize {

    public DietActivityChart(List<String> labels, List<Double> activityKcal) {
        setId("dietActivityChart_" + UUID.randomUUID());
        JreJsonObject labelsJson = toJson(labels);
        JreJsonObject activityJson = toJson(activityKcal);
        UI.getCurrent().getPage().executeJs(
                "window.renderDietActivityChart($0, $1, $2)",
                getElement(),
                labelsJson.get("data"),
                activityJson.get("data")
        );
    }

    static JreJsonObject toJson(List<?> values) {
        JreJsonObject obj = new JreJsonObject(new JreJsonFactory());
        JreJsonArray arr = new JreJsonArray(new JreJsonFactory());
        for (int i = 0; i < values.size(); i++) {
            arr.set(i, values.get(i) != null ? values.get(i).toString() : "null");
        }
        obj.set("data", arr);
        return obj;
    }
}
