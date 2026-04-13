package com.bervan.cookbook.service;

import com.bervan.cookbook.model.DietDay;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.ToDoubleFunction;

@Service
public class DietDashboardService {

    public enum GroupBy { DAY, WEEK, MONTH }

    public record DietChartData(
            List<String> labels,
            List<Double> activityKcal,
            List<Double> consumedKcal,
            List<Double> targetKcal,
            List<Double> effectiveTdee,
            List<Double> deficit,
            List<Double> weight
    ) {}

    private record RawDay(
            LocalDate date,
            double activityBurned,
            double consumed,
            double targetKcal,
            double effectiveTdee,
            double deficit,
            Double weight
    ) {}

    private final DietService dietService;

    public DietDashboardService(DietService dietService) {
        this.dietService = dietService;
    }

    public DietChartData getChartData(LocalDate from, LocalDate to, GroupBy groupBy) {
        List<DietDay> days = dietService.getRange(from, to);

        List<RawDay> raw = days.stream()
                .map(day -> {
                    double activityBurned = 0;
                    if (day.getActivityKcal() != null && day.getActivityKcalPercent() != null) {
                        activityBurned = day.getActivityKcal() * day.getActivityKcalPercent() / 100.0;
                    }
                    double consumed = dietService.totalKcal(day);
                    double tdee = day.getEstimatedDailyKcal() != null ? day.getEstimatedDailyKcal() : 0;
                    double target = day.getTargetKcal() != null ? day.getTargetKcal() : 0;
                    double effectiveTdee = tdee + activityBurned;
                    double deficit = effectiveTdee - consumed;
                    return new RawDay(day.getDate(), activityBurned, consumed, target, effectiveTdee, deficit, day.getWeightKg());
                })
                .sorted(Comparator.comparing(RawDay::date))
                .toList();

        return aggregate(raw, groupBy);
    }

    private DietChartData aggregate(List<RawDay> raw, GroupBy groupBy) {
        Map<String, List<RawDay>> grouped = new LinkedHashMap<>();
        for (RawDay d : raw) {
            grouped.computeIfAbsent(groupKey(d.date(), groupBy), k -> new ArrayList<>()).add(d);
        }

        List<String> labels = new ArrayList<>();
        List<Double> activityKcal = new ArrayList<>();
        List<Double> consumedKcal = new ArrayList<>();
        List<Double> targetKcal = new ArrayList<>();
        List<Double> effectiveTdee = new ArrayList<>();
        List<Double> deficit = new ArrayList<>();
        List<Double> weight = new ArrayList<>();

        for (Map.Entry<String, List<RawDay>> entry : grouped.entrySet()) {
            List<RawDay> g = entry.getValue();
            labels.add(entry.getKey());
            activityKcal.add(avg(g, RawDay::activityBurned));
            consumedKcal.add(avg(g, RawDay::consumed));
            targetKcal.add(avg(g, RawDay::targetKcal));
            effectiveTdee.add(avg(g, RawDay::effectiveTdee));
            deficit.add(avg(g, RawDay::deficit));
            OptionalDouble w = g.stream().filter(d -> d.weight() != null)
                    .mapToDouble(RawDay::weight).average();
            weight.add(w.isPresent() ? Math.round(w.getAsDouble() * 10.0) / 10.0 : null);
        }

        return new DietChartData(labels, activityKcal, consumedKcal, targetKcal, effectiveTdee, deficit, weight);
    }

    private String groupKey(LocalDate date, GroupBy groupBy) {
        return switch (groupBy) {
            case DAY -> date.toString();
            case WEEK -> date.getYear() + "-W"
                    + String.format("%02d", date.get(WeekFields.ISO.weekOfWeekBasedYear()));
            case MONTH -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        };
    }

    private double avg(List<RawDay> group, ToDoubleFunction<RawDay> extractor) {
        return Math.round(group.stream().mapToDouble(extractor).average().orElse(0) * 10.0) / 10.0;
    }
}
