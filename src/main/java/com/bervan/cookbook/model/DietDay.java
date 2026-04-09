package com.bervan.cookbook.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class DietDay extends BervanOwnedBaseEntity<UUID> {

    @Id
    private UUID id;

    private LocalDate date;

    private Integer targetKcal;
    private Integer targetProtein;
    private Integer targetCarbs;
    private Integer targetFat;
    private Integer targetFiber;
    private Integer activityKcal;
    private Double weightKg;

    private boolean deleted;
    private LocalDateTime modificationDate;

    @OneToMany(mappedBy = "dietDay", fetch = FetchType.EAGER,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private List<DietMeal> meals = new ArrayList<>();

    public DietDay() {
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public void setId(UUID id) { this.id = id; }

    @Override
    public LocalDateTime getModificationDate() { return modificationDate; }

    @Override
    public void setModificationDate(LocalDateTime modificationDate) { this.modificationDate = modificationDate; }

    @Override
    public Boolean isDeleted() { return deleted; }

    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getTargetKcal() { return targetKcal; }
    public void setTargetKcal(Integer targetKcal) { this.targetKcal = targetKcal; }

    public Integer getTargetProtein() { return targetProtein; }
    public void setTargetProtein(Integer targetProtein) { this.targetProtein = targetProtein; }

    public Integer getTargetCarbs() { return targetCarbs; }
    public void setTargetCarbs(Integer targetCarbs) { this.targetCarbs = targetCarbs; }

    public Integer getTargetFat() { return targetFat; }
    public void setTargetFat(Integer targetFat) { this.targetFat = targetFat; }

    public Integer getTargetFiber() { return targetFiber; }
    public void setTargetFiber(Integer targetFiber) { this.targetFiber = targetFiber; }

    public Integer getActivityKcal() { return activityKcal; }
    public void setActivityKcal(Integer activityKcal) { this.activityKcal = activityKcal; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public List<DietMeal> getMeals() { return meals; }
    public void setMeals(List<DietMeal> meals) { this.meals = meals; }
}
