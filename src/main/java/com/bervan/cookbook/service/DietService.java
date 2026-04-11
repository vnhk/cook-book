package com.bervan.cookbook.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.cookbook.model.*;
import com.bervan.cookbook.repository.DietDayRepository;
import com.bervan.cookbook.repository.DietMealItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class DietService extends BaseService<UUID, DietDay> {

    private final DietDayRepository dietDayRepository;
    private final DietMealItemRepository dietMealItemRepository;

    public DietService(DietDayRepository repository, SearchService searchService,
                       DietMealItemRepository dietMealItemRepository) {
        super(repository, searchService);
        this.dietDayRepository = repository;
        this.dietMealItemRepository = dietMealItemRepository;
    }

    public DietDay getOrCreateDay(LocalDate date) {
        return dietDayRepository.findByDateAndDeletedFalse(date).orElseGet(() -> {
            DietDay day = new DietDay();
            day.setId(UUID.randomUUID());
            day.setDate(date);
            dietDayRepository.findByDateAndDeletedFalse(date.minusDays(1)).ifPresent(prev -> {
                day.setTargetKcal(prev.getTargetKcal());
                day.setTargetProtein(prev.getTargetProtein());
                day.setTargetCarbs(prev.getTargetCarbs());
                day.setTargetFat(prev.getTargetFat());
                day.setTargetFiber(prev.getTargetFiber());
                day.setActivityKcal(prev.getActivityKcal());
                day.setAge(prev.getAge());
                day.setGender(prev.getGender());
                day.setHeightCm(prev.getHeightCm());
                day.setActivityLevel(prev.getActivityLevel());
            });
            return save(day);
        });
    }

    public DietMeal getOrCreateMeal(DietDay day, DietMeal.MealType type) {
        return day.getMeals().stream()
                .filter(m -> m.getMealType() == type && !Boolean.TRUE.equals(m.isDeleted()))
                .findFirst()
                .orElseGet(() -> {
                    DietMeal meal = new DietMeal();
                    meal.setId(UUID.randomUUID());
                    meal.setMealType(type);
                    meal.setDietDay(day);
                    day.getOwners().forEach(meal::addOwner);
                    day.getMeals().add(meal);
                    save(day);
                    return day.getMeals().stream()
                            .filter(m -> m.getMealType() == type && !Boolean.TRUE.equals(m.isDeleted()))
                            .findFirst().orElseThrow();
                });
    }

    public void addItemToMeal(DietDay day, DietMeal meal, DietMealItem item) {
        item.setId(UUID.randomUUID());
        item.setMeal(meal);
        day.getOwners().forEach(item::addOwner);
        meal.getItems().add(item);
        save(day);
    }

    public void removeItem(DietDay day, DietMealItem item) {
        day.getMeals().forEach(meal -> meal.getItems().removeIf(i -> i.getId().equals(item.getId())));
        item.setDeleted(true);
        dietMealItemRepository.save(item);
        save(day);
    }

    public void updateDayTargets(DietDay day, Integer targetKcal, Integer targetProtein,
                                  Integer targetCarbs, Integer targetFat, Integer targetFiber,
                                  Integer activityKcal, Double weightKg, String notes,
                                  Integer age, String gender, Integer heightCm, String activityLevel) {
        day.setTargetKcal(targetKcal);
        day.setTargetProtein(targetProtein);
        day.setTargetCarbs(targetCarbs);
        day.setTargetFat(targetFat);
        day.setTargetFiber(targetFiber);
        day.setActivityKcal(activityKcal);
        day.setWeightKg(weightKg);
        day.setNotes(notes);
        day.setAge(age);
        day.setGender(gender);
        day.setHeightCm(heightCm);
        day.setActivityLevel(activityLevel);
        save(day);
    }

    public void copyMealItems(DietDay targetDay, DietMeal.MealType targetType,
                               LocalDate sourceDate, DietMeal.MealType sourceType) {
        Optional<DietDay> sourceOpt = findByDate(sourceDate);
        if (sourceOpt.isEmpty()) return;
        DietDay sourceDay = sourceOpt.get();
        DietMeal sourceMeal = sourceDay.getMeals().stream()
                .filter(m -> m.getMealType() == sourceType && !Boolean.TRUE.equals(m.isDeleted()))
                .findFirst().orElse(null);
        if (sourceMeal == null) return;

        java.util.List<DietMealItem> sourceItems = sourceMeal.getItems().stream()
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .toList();
        if (sourceItems.isEmpty()) return;

        getOrCreateMeal(targetDay, targetType);
        DietDay freshTarget = getOrCreateDay(targetDay.getDate());
        DietMeal targetMeal = freshTarget.getMeals().stream()
                .filter(m -> m.getMealType() == targetType && !Boolean.TRUE.equals(m.isDeleted()))
                .findFirst().orElseThrow();

        for (DietMealItem source : sourceItems) {
            DietMealItem copy = new DietMealItem();
            copy.setId(UUID.randomUUID());
            copy.setDescription(source.getDescription());
            copy.setKcal(source.getKcal());
            copy.setProtein(source.getProtein());
            copy.setFat(source.getFat());
            copy.setCarbs(source.getCarbs());
            copy.setFiber(source.getFiber());
            copy.setIngredient(source.getIngredient());
            copy.setAmountGrams(source.getAmountGrams());
            copy.setMeal(targetMeal);
            freshTarget.getOwners().forEach(copy::addOwner);
            targetMeal.getItems().add(copy);
        }
        save(freshTarget);
    }

    public Optional<DietDay> findByDate(LocalDate date) {
        return dietDayRepository.findByDateAndDeletedFalse(date);
    }

    public double totalKcal(DietDay day) {
        return day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .flatMap(m -> m.getItems().stream())
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .mapToDouble(DietMealItem::getEffectiveKcal)
                .sum();
    }

    public double totalProtein(DietDay day) {
        return day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .flatMap(m -> m.getItems().stream())
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .mapToDouble(DietMealItem::getEffectiveProtein)
                .sum();
    }

    public double totalFat(DietDay day) {
        return day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .flatMap(m -> m.getItems().stream())
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .mapToDouble(DietMealItem::getEffectiveFat)
                .sum();
    }

    public double totalCarbs(DietDay day) {
        return day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .flatMap(m -> m.getItems().stream())
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .mapToDouble(DietMealItem::getEffectiveCarbs)
                .sum();
    }

    public double totalFiber(DietDay day) {
        return day.getMeals().stream()
                .filter(m -> !Boolean.TRUE.equals(m.isDeleted()))
                .flatMap(m -> m.getItems().stream())
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .mapToDouble(DietMealItem::getEffectiveFiber)
                .sum();
    }
}
