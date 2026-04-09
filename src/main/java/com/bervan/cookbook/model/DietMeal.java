package com.bervan.cookbook.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class DietMeal extends BervanOwnedBaseEntity<UUID> {

    public enum MealType {
        BREAKFAST("Breakfast"),
        LUNCH("Lunch"),
        DINNER("Dinner"),
        SNACK("Snack"),
        OTHER("Other");

        private final String displayName;
        MealType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private MealType mealType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diet_day_id")
    private DietDay dietDay;

    private boolean deleted;
    private LocalDateTime modificationDate;

    @OneToMany(mappedBy = "meal", fetch = FetchType.EAGER,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private List<DietMealItem> items = new ArrayList<>();

    public DietMeal() {}

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

    public MealType getMealType() { return mealType; }
    public void setMealType(MealType mealType) { this.mealType = mealType; }

    public DietDay getDietDay() { return dietDay; }
    public void setDietDay(DietDay dietDay) { this.dietDay = dietDay; }

    public List<DietMealItem> getItems() { return items; }
    public void setItems(List<DietMealItem> items) { this.items = items; }
}
