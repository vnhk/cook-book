package com.bervan.cookbook;

import com.bervan.cookbook.model.DietDay;
import com.bervan.cookbook.model.DietMeal;
import com.bervan.cookbook.model.DietMealItem;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.service.DietDashboardService;
import com.bervan.cookbook.service.DietService;
import com.bervan.cookbook.service.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cook-book/diet")
public class DietRestController {

    private final DietService dietService;
    private final DietDashboardService dashboardService;
    private final IngredientService ingredientService;

    public DietRestController(DietService dietService, DietDashboardService dashboardService,
                               IngredientService ingredientService) {
        this.dietService = dietService;
        this.dashboardService = dashboardService;
        this.ingredientService = ingredientService;
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    record DietMealItemDto(UUID id, String displayName, String description,
                           UUID ingredientId, String ingredientName, Double amountGrams,
                           double kcal, double protein, double fat, double carbs, double fiber,
                           boolean quickEntry) {}

    record DietMealDto(UUID id, String mealType, String mealTypeName,
                       List<DietMealItemDto> items,
                       double totalKcal, double totalProtein, double totalFat,
                       double totalCarbs, double totalFiber) {}

    record DietDayDto(String date,
                      Integer targetKcal, Integer estimatedDailyKcal,
                      Integer targetProtein, Integer targetCarbs, Integer targetFat, Integer targetFiber,
                      Integer activityKcal, Integer activityKcalPercent,
                      Double weightKg, String notes,
                      Integer age, String gender, Integer heightCm, String activityLevel,
                      double totalKcal, double totalProtein, double totalFat,
                      double totalCarbs, double totalFiber,
                      List<DietMealDto> meals) {}

    record DietChartDataDto(List<String> labels, List<Double> activityKcal, List<Double> consumedKcal,
                             List<Double> targetKcal, List<Double> effectiveTdee,
                             List<Double> deficit, List<Double> weight) {}

    record MacroBreakdownDto(double avgConsumedProtein, double avgConsumedFat, double avgConsumedCarbs,
                              double avgTargetProtein, double avgTargetFat, double avgTargetCarbs,
                              boolean hasData) {}

    record WeightProjectionDto(List<String> labels, List<Double> actualWeight, List<Double> projectedWeight,
                                double avgDailyDeficit, double weeklyWeightChange) {}

    record DashboardDto(DietChartDataDto chartData, MacroBreakdownDto macroBreakdown,
                        WeightProjectionDto weightProjection) {}

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DietMealItemDto toItemDto(DietMealItem i) {
        boolean quickEntry = i.getIngredient() == null;
        return new DietMealItemDto(
                i.getId(), i.getDisplayName(), i.getDescription(),
                i.getIngredient() != null ? i.getIngredient().getId() : null,
                i.getIngredient() != null ? i.getIngredient().getName() : null,
                i.getAmountGrams(),
                round1(i.getEffectiveKcal()), round1(i.getEffectiveProtein()),
                round1(i.getEffectiveFat()), round1(i.getEffectiveCarbs()), round1(i.getEffectiveFiber()),
                quickEntry
        );
    }

    private DietMealDto toMealDto(DietMeal m) {
        List<DietMealItemDto> items = m.getItems().stream()
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .map(this::toItemDto)
                .collect(Collectors.toList());
        double tk = items.stream().mapToDouble(DietMealItemDto::kcal).sum();
        double tp = items.stream().mapToDouble(DietMealItemDto::protein).sum();
        double tf = items.stream().mapToDouble(DietMealItemDto::fat).sum();
        double tc = items.stream().mapToDouble(DietMealItemDto::carbs).sum();
        double tfi = items.stream().mapToDouble(DietMealItemDto::fiber).sum();
        return new DietMealDto(m.getId(), m.getMealType().name(), m.getMealType().getDisplayName(),
                items, round1(tk), round1(tp), round1(tf), round1(tc), round1(tfi));
    }

    private DietDayDto toDayDto(DietDay day) {
        List<DietMealDto> meals = day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .sorted(Comparator.comparing(m -> m.getMealType().ordinal()))
                .map(this::toMealDto)
                .collect(Collectors.toList());
        return new DietDayDto(
                day.getDate().toString(),
                day.getTargetKcal(), day.getEstimatedDailyKcal(),
                day.getTargetProtein(), day.getTargetCarbs(), day.getTargetFat(), day.getTargetFiber(),
                day.getActivityKcal(), day.getActivityKcalPercent(),
                day.getWeightKg(), day.getNotes(),
                day.getAge(), day.getGender(), day.getHeightCm(), day.getActivityLevel(),
                round1(dietService.totalKcal(day)), round1(dietService.totalProtein(day)),
                round1(dietService.totalFat(day)), round1(dietService.totalCarbs(day)),
                round1(dietService.totalFiber(day)),
                meals
        );
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // ─── Day ─────────────────────────────────────────────────────────────────

    @GetMapping("/day")
    public ResponseEntity<DietDayDto> getDay(@RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
    }

    @PutMapping("/day")
    public ResponseEntity<DietDayDto> updateDay(@RequestParam String date,
                                                 @RequestBody Map<String, Object> req) {
        LocalDate d = LocalDate.parse(date);
        DietDay day = dietService.getOrCreateDay(d);
        dietService.updateDayTargets(day,
                intVal(req, "targetKcal"),
                intVal(req, "estimatedDailyKcal"),
                intVal(req, "targetProtein"),
                intVal(req, "targetCarbs"),
                intVal(req, "targetFat"),
                intVal(req, "targetFiber"),
                intVal(req, "activityKcal"),
                intVal(req, "activityKcalPercent"),
                doubleVal(req, "weightKg"),
                (String) req.get("notes"),
                intVal(req, "age"),
                (String) req.get("gender"),
                intVal(req, "heightCm"),
                (String) req.get("activityLevel")
        );
        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
    }

    // ─── Items ────────────────────────────────────────────────────────────────

    @PostMapping("/day/{date}/meals/{mealType}/items")
    public ResponseEntity<DietDayDto> addItem(@PathVariable String date,
                                               @PathVariable String mealType,
                                               @RequestBody Map<String, Object> req) {
        LocalDate d = LocalDate.parse(date);
        DietMeal.MealType type;
        try { type = DietMeal.MealType.valueOf(mealType.toUpperCase()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }

        DietDay day = dietService.getOrCreateDay(d);
        DietMeal meal = dietService.getOrCreateMeal(day, type);

        DietMealItem item = new DietMealItem();
        item.setModificationDate(java.time.LocalDateTime.now());
        item.setDeleted(false);

        String ingredientIdStr = (String) req.get("ingredientId");
        if (ingredientIdStr != null) {
            Optional<Ingredient> ing = ingredientService.loadById(UUID.fromString(ingredientIdStr));
            ing.ifPresent(item::setIngredient);
        }
        if (req.get("amountGrams") != null)
            item.setAmountGrams(((Number) req.get("amountGrams")).doubleValue());
        item.setDescription((String) req.get("description"));
        if (req.get("kcal") != null)
            item.setKcal(((Number) req.get("kcal")).doubleValue());
        if (req.get("protein") != null)
            item.setProtein(((Number) req.get("protein")).doubleValue());
        if (req.get("fat") != null)
            item.setFat(((Number) req.get("fat")).doubleValue());
        if (req.get("carbs") != null)
            item.setCarbs(((Number) req.get("carbs")).doubleValue());
        if (req.get("fiber") != null)
            item.setFiber(((Number) req.get("fiber")).doubleValue());

        DietDay freshDay = dietService.getOrCreateDay(d);
        DietMeal freshMeal = freshDay.getMeals().stream()
                .filter(m -> m.getMealType() == type && !Boolean.TRUE.equals(m.isDeleted()))
                .findFirst().orElseGet(() -> dietService.getOrCreateMeal(freshDay, type));

        dietService.addItemToMeal(freshDay, freshMeal, item);
        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
    }

    @DeleteMapping("/day/{date}/items/{itemId}")
    public ResponseEntity<DietDayDto> removeItem(@PathVariable String date,
                                                  @PathVariable UUID itemId) {
        LocalDate d = LocalDate.parse(date);
        DietDay day = dietService.getOrCreateDay(d);
        day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .flatMap(m -> m.getItems().stream())
                .filter(i -> i.getId().equals(itemId) && !Boolean.TRUE.equals(i.isDeleted()))
                .findFirst()
                .ifPresent(item -> dietService.removeItem(day, item));
        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
    }

    @PostMapping("/day/{date}/meals/{mealType}/copy")
    public ResponseEntity<DietDayDto> copyMeal(@PathVariable String date,
                                                @PathVariable String mealType,
                                                @RequestParam String sourceDate,
                                                @RequestParam String sourceType) {
        LocalDate d = LocalDate.parse(date);
        DietMeal.MealType targetMealType, sourceMealType;
        try {
            targetMealType = DietMeal.MealType.valueOf(mealType.toUpperCase());
            sourceMealType = DietMeal.MealType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }

        DietDay day = dietService.getOrCreateDay(d);
        dietService.copyMealItems(day, targetMealType, LocalDate.parse(sourceDate), sourceMealType);
        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
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
        try { gb = DietDashboardService.GroupBy.valueOf(groupBy.toUpperCase()); }
        catch (IllegalArgumentException e) { gb = DietDashboardService.GroupBy.DAY; }

        DietDashboardService.DietChartData data = dashboardService.getChartData(fromDate, toDate, gb);
        DietDashboardService.MacroBreakdownData macro = dashboardService.getMacroBreakdown(fromDate, toDate);
        DietDashboardService.WeightProjectionData proj = dashboardService.getWeightProjectionData();

        DietChartDataDto chartDto = new DietChartDataDto(data.labels(), data.activityKcal(),
                data.consumedKcal(), data.targetKcal(), data.effectiveTdee(), data.deficit(), data.weight());
        MacroBreakdownDto macroDto = new MacroBreakdownDto(macro.avgConsumedProtein(), macro.avgConsumedFat(),
                macro.avgConsumedCarbs(), macro.avgTargetProtein(), macro.avgTargetFat(), macro.avgTargetCarbs(),
                macro.hasData());
        WeightProjectionDto projDto = new WeightProjectionDto(proj.labels(), proj.actualWeight(),
                proj.projectedWeight(), proj.avgDailyDeficit(), proj.weeklyWeightChange());

        return ResponseEntity.ok(new DashboardDto(chartDto, macroDto, projDto));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Integer intVal(Map<String, Object> req, String key) {
        Object v = req.get(key);
        if (v == null) return null;
        return ((Number) v).intValue();
    }

    private Double doubleVal(Map<String, Object> req, String key) {
        Object v = req.get(key);
        if (v == null) return null;
        return ((Number) v).doubleValue();
    }
}
