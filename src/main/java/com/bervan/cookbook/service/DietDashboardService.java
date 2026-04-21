package com.bervan.cookbook.service;

import com.bervan.cookbook.model.DietDay;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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
                    double target = (day.getTargetKcal() != null ? day.getTargetKcal() : 0) + activityBurned;
                    double deficit = tdee - consumed;
                    return new RawDay(day.getDate(), activityBurned, consumed, target, tdee, deficit, day.getWeightKg());
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

    // ---- Weight Projection ----

    public record WeightProjectionData(
            List<String> labels,
            List<Double> actualWeight,
            List<Double> projectedWeight,
            double avgDailyDeficit,
            double weeklyWeightChange
    ) {}

    /**
     * Returns weekly weight history (last 26 weeks) + 13-week projection based on
     * the average daily deficit calculated from the last 90 days.
     */
    public WeightProjectionData getWeightProjectionData() {
        LocalDate today = LocalDate.now();
        LocalDate histStart = today.minusWeeks(26);
        LocalDate projEnd = today.plusWeeks(13);

        List<DietDay> histDays = dietService.getRange(histStart, today);

        // Avg daily deficit from last 90 days (only days with TDEE set)
        LocalDate deficitStart = today.minusDays(90);
        List<DietDay> recentDays = histDays.stream()
                .filter(d -> !d.getDate().isBefore(deficitStart)
                        && d.getEstimatedDailyKcal() != null
                        && d.getEstimatedDailyKcal() > 0)
                .toList();

        double avgDailyDeficit = recentDays.stream()
                .mapToDouble(d -> {
                    double tdee = d.getEstimatedDailyKcal();
                    double act = 0;
                    if (d.getActivityKcal() != null && d.getActivityKcalPercent() != null) {
                        act = d.getActivityKcal() * d.getActivityKcalPercent() / 100.0;
                    }
                    return tdee + act - dietService.totalKcal(d);
                })
                .average()
                .orElse(0);

        // Weekly weight change: 1 kg fat ≈ 7700 kcal
        double weeklyWeightChange = Math.round((avgDailyDeficit * 7.0 / 7700.0) * 100.0) / 100.0;

        // Group historical data by ISO week start (Monday) → avg weight
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd MMM");
        Map<LocalDate, List<Double>> weekWeightMap = new LinkedHashMap<>();
        for (DietDay d : histDays) {
            if (d.getWeightKg() == null) continue;
            LocalDate weekStart = d.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weekWeightMap.computeIfAbsent(weekStart, k -> new ArrayList<>()).add(d.getWeightKg());
        }

        // Find last known weight
        Double lastKnownWeight = histDays.stream()
                .filter(d -> d.getWeightKg() != null)
                .max(Comparator.comparing(DietDay::getDate))
                .map(DietDay::getWeightKg)
                .orElse(null);

        // Build historical weekly labels (every Monday from histStart to this week)
        List<LocalDate> histWeekStarts = new ArrayList<>();
        LocalDate cursor = histStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        while (!cursor.isAfter(thisWeekStart)) {
            histWeekStarts.add(cursor);
            cursor = cursor.plusWeeks(1);
        }

        // Build future weekly labels (+1w … +13w)
        List<LocalDate> futureWeekStarts = new ArrayList<>();
        LocalDate nextWeek = thisWeekStart.plusWeeks(1);
        for (int i = 0; i < 13; i++) {
            futureWeekStarts.add(nextWeek.plusWeeks(i));
        }

        List<String> labels = new ArrayList<>();
        List<Double> actualWeight = new ArrayList<>();
        List<Double> projectedWeight = new ArrayList<>();

        // Historical part
        for (LocalDate ws : histWeekStarts) {
            labels.add(ws.format(labelFmt));
            List<Double> ww = weekWeightMap.get(ws);
            Double avg = (ww != null && !ww.isEmpty())
                    ? Math.round(ww.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 10.0) / 10.0
                    : null;
            actualWeight.add(avg);
            // Projected starts only at the junction point (last hist week)
            if (ws.equals(thisWeekStart)) {
                projectedWeight.add(lastKnownWeight);
            } else {
                projectedWeight.add(null);
            }
        }

        // Future projection part
        if (lastKnownWeight != null) {
            for (int i = 0; i < futureWeekStarts.size(); i++) {
                LocalDate ws = futureWeekStarts.get(i);
                labels.add(ws.format(labelFmt));
                actualWeight.add(null);
                double proj = Math.round((lastKnownWeight - weeklyWeightChange * (i + 1)) * 100.0) / 100.0;
                projectedWeight.add(proj);
            }
        }

        return new WeightProjectionData(labels, actualWeight, projectedWeight, avgDailyDeficit, weeklyWeightChange);
    }
}
