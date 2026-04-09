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
            day.setActivityKcal(0);
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
                    meal.getOwners().addAll(day.getOwners());
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
        item.getOwners().addAll(day.getOwners());
        meal.getItems().add(item);
        save(day);
    }

    public void removeItem(DietDay day, DietMealItem item) {
        day.getMeals().forEach(meal -> meal.getItems().removeIf(i -> i.getId().equals(item.getId())));
        item.setDeleted(true);
        dietMealItemRepository.save(item);
        save(day);
    }

    public void updateDayTargets(DietDay day, Integer targetKcal, Integer targetProtein, Integer activityKcal, Double weightKg) {
        day.setTargetKcal(targetKcal);
        day.setTargetProtein(targetProtein);
        day.setActivityKcal(activityKcal);
        day.setWeightKg(weightKg);
        save(day);
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
}
