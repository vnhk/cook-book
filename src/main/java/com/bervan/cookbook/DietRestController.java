package com.bervan.cookbook;

import com.bervan.cookbook.model.DietDay;
import com.bervan.cookbook.model.DietMeal;
import com.bervan.cookbook.model.DietMealItem;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.service.DietService;
import com.bervan.cookbook.service.IngredientService;
import com.bervan.logging.JsonLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cook-book/diet")
public class DietRestController {

    private final DietService dietService;
    private final IngredientService ingredientService;
    private final JsonLogger log = JsonLogger.getLogger(DietRestController.class, "cook-book");

    public DietRestController(DietService dietService, IngredientService ingredientService) {
        this.dietService = dietService;
        this.ingredientService = ingredientService;
    }


    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DietMealItemDto toItemDto(DietMealItem i) {
        boolean quickEntry = i.getIngredient() == null;
        DietMealItemDto dto = new DietMealItemDto();
        dto.setId(i.getId());
        dto.setDisplayName(i.getDisplayName());
        dto.setDescription(i.getDescription());
        dto.setIngredientId(i.getIngredient() != null ? i.getIngredient().getId() : null);
        dto.setIngredientName(i.getIngredient() != null ? i.getIngredient().getName() : null);
        dto.setAmountGrams(i.getAmountGrams());
        dto.setKcal(round1(i.getEffectiveKcal()));
        dto.setProtein(round1(i.getEffectiveProtein()));
        dto.setFat(round1(i.getEffectiveFat()));
        dto.setCarbs(round1(i.getEffectiveCarbs()));
        dto.setFiber(round1(i.getEffectiveFiber()));
        dto.setQuickEntry(quickEntry);
        return dto;
    }

    private DietMealDto toMealDto(DietMeal m) {
        List<DietMealItemDto> items = m.getItems().stream()
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .map(this::toItemDto)
                .collect(Collectors.toList());
        double tk = items.stream().mapToDouble(DietMealItemDto::getKcal).sum();
        double tp = items.stream().mapToDouble(DietMealItemDto::getProtein).sum();
        double tf = items.stream().mapToDouble(DietMealItemDto::getFat).sum();
        double tc = items.stream().mapToDouble(DietMealItemDto::getCarbs).sum();
        double tfi = items.stream().mapToDouble(DietMealItemDto::getFiber).sum();
        DietMealDto dto = new DietMealDto();
        dto.setId(m.getId());
        dto.setMealType(m.getMealType().name());
        dto.setMealTypeName(m.getMealType().getDisplayName());
        dto.setItems(items);
        dto.setTotalKcal(round1(tk));
        dto.setTotalProtein(round1(tp));
        dto.setTotalFat(round1(tf));
        dto.setTotalCarbs(round1(tc));
        dto.setTotalFiber(round1(tfi));
        return dto;
    }

    private DietDayDto toDayDto(DietDay day) {
        List<DietMealDto> meals = day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .sorted(Comparator.comparing(m -> m.getMealType().ordinal()))
                .map(this::toMealDto)
                .collect(Collectors.toList());
        DietDayDto dto = new DietDayDto();
        dto.setId(day.getId());
        dto.setDate(day.getDate().toString());
        dto.setTargetKcal(day.getTargetKcal());
        dto.setEstimatedDailyKcal(day.getEstimatedDailyKcal());
        dto.setTargetProtein(day.getTargetProtein());
        dto.setTargetCarbs(day.getTargetCarbs());
        dto.setTargetFat(day.getTargetFat());
        dto.setTargetFiber(day.getTargetFiber());
        dto.setActivityKcal(day.getActivityKcal());
        dto.setActivityKcalPercent(day.getActivityKcalPercent());
        dto.setWeightKg(day.getWeightKg());
        dto.setNotes(day.getNotes());
        dto.setAge(day.getAge());
        dto.setGender(day.getGender());
        dto.setHeightCm(day.getHeightCm());
        dto.setActivityLevel(day.getActivityLevel());
        dto.setTotalKcal(round1(dietService.totalKcal(day)));
        dto.setTotalProtein(round1(dietService.totalProtein(day)));
        dto.setTotalFat(round1(dietService.totalFat(day)));
        dto.setTotalCarbs(round1(dietService.totalCarbs(day)));
        dto.setTotalFiber(round1(dietService.totalFiber(day)));
        dto.setMeals(meals);
        return dto;
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

    @PostMapping("/recreate-day/{date}")
    public ResponseEntity<DietDayDto> recreateDay(@PathVariable String date) {
        LocalDate d = LocalDate.parse(date);
        dietService.deleteDay(d);
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
        try {
            type = DietMeal.MealType.valueOf(mealType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        DietMealItem item = new DietMealItem();
        item.setModificationDate(LocalDateTime.now());
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


    @PostMapping("/day/{date}/meals/auto")
    public ResponseEntity<DietDayDto> autoMeals(@PathVariable String date,
                                                @RequestBody Map<String, Integer> req) {
        LocalDate d = LocalDate.parse(date);
        DietDay day = dietService.getOrCreateDay(d);

        boolean valid = validateAutoMeals(req.get("kcalPercentage"), d)
                && validateAutoMeals(req.get("proteinPercentage"), d)
                && validateAutoMeals(req.get("fatPercentage"), d)
                && validateAutoMeals(req.get("carbsPercentage"), d)
                && validateAutoMeals(req.get("fiberPercentage"), d);

        if (!valid) {
            return ResponseEntity.badRequest().body(toDayDto(day));
        }

        if (day.getTargetKcal() == null || day.getTargetProtein() == null || day.getTargetCarbs() == null || day.getTargetFiber() == null) {
            log.error("Cannot auto-generate meals for day {}: missing targets", d);
            return ResponseEntity.badRequest().body(toDayDto(day));
        }

        // Remove all existing meals items
        List<DietMealItem> itemsToRemove = new ArrayList<>();
        DietDay finalDay = day;
        day.getMeals().stream().forEach(m -> {
            for (DietMealItem item : m.getItems()) {
                itemsToRemove.add(item);
            }
        });
        itemsToRemove.forEach(i -> removeMealItem(i.getId(), finalDay));

        day = dietService.getOrCreateDay(d); //refresh

        DietMeal.MealType[] types = DietMeal.MealType.values();
        int amountOfMeals = types.length;

        for (int i = 0; i < amountOfMeals; i++) {
            DietMeal meal = dietService.getOrCreateMeal(day, types[i]);
            DietMealItem item = new DietMealItem();
            item.setModificationDate(LocalDateTime.now());
            item.setDeleted(false);
            item.setDescription("Auto-generated meal");
            item.setKcal(round1((double) day.getTargetKcal() * req.get("kcalPercentage") / 100 / amountOfMeals));
            item.setCarbs(round1((double) day.getTargetCarbs() * req.get("carbsPercentage") / 100 / amountOfMeals));
            item.setProtein(round1((double) day.getTargetProtein() * req.get("proteinPercentage") / 100 / amountOfMeals));
            item.setFat(round1((double) day.getTargetFat() * req.get("fatPercentage") / 100 / amountOfMeals));
            item.setFiber(round1((double) day.getTargetFiber() * req.get("fiberPercentage") / 100 / amountOfMeals));
            dietService.addItemToMeal(day, meal, item);
        }

        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
    }

    private boolean validateAutoMeals(Integer percentage, LocalDate d) {
        if (percentage == null) {
            log.error("Cannot auto-generate meals for day {}: missing percentage", d);
            return false;
        }
        if (percentage < 1 || percentage > 1000) {
            log.error("Cannot auto-generate meals for day {}: invalid percentage", d);
            return false;
        }
        return true;
    }

    @DeleteMapping("/day/{date}/items/{itemId}")
    public ResponseEntity<DietDayDto> removeItem(@PathVariable String date,
                                                 @PathVariable UUID itemId) {
        LocalDate d = LocalDate.parse(date);
        DietDay day = dietService.getOrCreateDay(d);
        removeMealItem(itemId, day);
        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
    }

    private void removeMealItem(UUID itemId, DietDay day) {
        day.getMeals().stream()
                .filter(m -> Boolean.FALSE.equals(m.isDeleted()))
                .flatMap(m -> m.getItems().stream())
                .filter(i -> i.getId().equals(itemId) && Boolean.FALSE.equals(i.isDeleted()))
                .findFirst()
                .ifPresent(item -> dietService.removeItem(day, item));
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        DietDay day = dietService.getOrCreateDay(d);
        dietService.copyMealItems(day, targetMealType, LocalDate.parse(sourceDate), sourceMealType);
        return ResponseEntity.ok(toDayDto(dietService.getOrCreateDay(d)));
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
