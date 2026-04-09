package com.bervan.cookbook.view;

import com.bervan.cookbook.model.DietDay;
import com.bervan.cookbook.model.DietMeal;
import com.bervan.cookbook.model.DietMealItem;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.service.DietService;
import com.bervan.cookbook.service.IngredientService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractDietView extends VerticalLayout {

    public static final String ROUTE_NAME = "/cook-book/diet";

    private final DietService dietService;
    private final IngredientService ingredientService;

    private DietDay currentDay;
    private LocalDate selectedDate = LocalDate.now();

    private Div summaryCard;
    private VerticalLayout mealsLayout;

    public AbstractDietView(DietService dietService, IngredientService ingredientService) {
        this.dietService = dietService;
        this.ingredientService = ingredientService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new CookBookPageLayout(ROUTE_NAME));
        add(buildDateBar());

        summaryCard = new Div();
        summaryCard.getStyle().set("width", "100%");
        add(summaryCard);

        mealsLayout = new VerticalLayout();
        mealsLayout.setPadding(false);
        mealsLayout.setSpacing(true);
        mealsLayout.setWidth("100%");
        add(mealsLayout);

        loadDay(selectedDate);
    }

    private HorizontalLayout buildDateBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(Alignment.CENTER);

        Button prev = new Button(VaadinIcon.ARROW_LEFT.create(), e -> {
            selectedDate = selectedDate.minusDays(1);
            loadDay(selectedDate);
        });
        Button next = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> {
            selectedDate = selectedDate.plusDays(1);
            loadDay(selectedDate);
        });
        Button today = new Button("Today", e -> {
            selectedDate = LocalDate.now();
            loadDay(selectedDate);
        });

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(selectedDate);
        datePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedDate = e.getValue();
                loadDay(selectedDate);
            }
        });

        Button goalsBtn = new Button("Set Goals", VaadinIcon.COG.create(), e -> openGoalsDialog());
        goalsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        bar.add(prev, datePicker, next, today, goalsBtn);
        return bar;
    }

    private void loadDay(LocalDate date) {
        currentDay = dietService.getOrCreateDay(date);
        refresh();
    }

    private void refresh() {
        summaryCard.removeAll();
        summaryCard.add(buildSummaryCard());

        mealsLayout.removeAll();
        for (DietMeal.MealType type : DietMeal.MealType.values()) {
            mealsLayout.add(buildMealSection(type));
        }
    }

    private Div buildSummaryCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--bervan-surface-1)")
                .set("border", "1px solid var(--bervan-border-color)")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("margin-bottom", "8px");

        double consumed = dietService.totalKcal(currentDay);
        double protein = dietService.totalProtein(currentDay);
        double fat = dietService.totalFat(currentDay);
        double carbs = dietService.totalCarbs(currentDay);

        int targetKcal = currentDay.getTargetKcal() != null ? currentDay.getTargetKcal() : 0;
        int targetProtein = currentDay.getTargetProtein() != null ? currentDay.getTargetProtein() : 0;
        int activity = currentDay.getActivityKcal() != null ? currentDay.getActivityKcal() : 0;

        double remainingKcal = targetKcal + activity - consumed;
        double remainingProtein = targetProtein - protein;

        String dateLabel = currentDay.getDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
        H3 title = new H3(dateLabel);
        title.getStyle().set("margin", "0 0 12px 0");

        HorizontalLayout macroRow = new HorizontalLayout();
        macroRow.setSpacing(true);
        macroRow.setWidthFull();

        macroRow.add(
                buildMacroTile("Calories", fmt(consumed), "kcal", targetKcal > 0 ? "/ " + targetKcal : "", false),
                buildMacroTile("Remaining", fmt(remainingKcal), "kcal", activity > 0 ? "(+" + activity + " activity)" : "", remainingKcal < 0),
                buildMacroTile("Protein", fmt(protein) + "g", "", targetProtein > 0 ? "/ " + targetProtein + "g" : "", false),
                buildMacroTile("Protein left", fmt(remainingProtein) + "g", "", "", remainingProtein < 0),
                buildMacroTile("Fat", fmt(fat) + "g", "", "", false),
                buildMacroTile("Carbs", fmt(carbs) + "g", "", "", false)
        );

        card.add(title, macroRow);
        return card;
    }

    private Div buildMacroTile(String label, String value, String unit, String sub, boolean warning) {
        Div tile = new Div();
        tile.getStyle()
                .set("background", warning ? "rgba(var(--bervan-danger-rgb, 239,68,68), 0.15)" : "var(--bervan-surface-2)")
                .set("border", "1px solid var(--bervan-border-color)")
                .set("border-radius", "6px")
                .set("padding", "10px 14px")
                .set("min-width", "100px")
                .set("text-align", "center");

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-size", "var(--bervan-font-size-sm)").set("color", "var(--bervan-text-secondary)").set("display", "block");

        Span valueSpan = new Span(value + (unit.isEmpty() ? "" : " " + unit));
        valueSpan.getStyle().set("font-size", "var(--bervan-font-size-lg)").set("font-weight", "bold").set("display", "block")
                .set("color", warning ? "rgb(var(--bervan-danger-rgb, 239,68,68))" : "var(--bervan-text-primary)");

        tile.add(labelSpan, valueSpan);
        if (!sub.isEmpty()) {
            Span subSpan = new Span(sub);
            subSpan.getStyle().set("font-size", "var(--bervan-font-size-xs)").set("color", "var(--bervan-text-tertiary)").set("display", "block");
            tile.add(subSpan);
        }
        return tile;
    }

    private Div buildMealSection(DietMeal.MealType type) {
        Div section = new Div();
        section.getStyle()
                .set("background", "var(--bervan-surface-1)")
                .set("border", "1px solid var(--bervan-border-color)")
                .set("border-radius", "8px")
                .set("padding", "12px 16px")
                .set("margin-bottom", "8px");

        DietMeal meal = currentDay.getMeals().stream()
                .filter(m -> m.getMealType() == type && !Boolean.TRUE.equals(m.isDeleted()))
                .findFirst().orElse(null);

        List<DietMealItem> items = meal != null ? meal.getItems().stream()
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .toList() : List.of();

        double mealKcal = items.stream().mapToDouble(DietMealItem::getEffectiveKcal).sum();
        double mealProtein = items.stream().mapToDouble(DietMealItem::getEffectiveProtein).sum();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H4 mealTitle = new H4(type.getDisplayName());
        mealTitle.getStyle().set("margin", "0");

        HorizontalLayout mealInfo = new HorizontalLayout();
        mealInfo.setSpacing(false);
        Span kcalSpan = new Span(fmt(mealKcal) + " kcal");
        kcalSpan.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--lumo-font-size-s)").set("margin-right", "8px");
        Span proteinSpan = new Span(fmt(mealProtein) + "g protein");
        proteinSpan.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--lumo-font-size-s)");
        mealInfo.add(kcalSpan, proteinSpan);

        Button addBtn = new Button("Add", VaadinIcon.PLUS.create(), e -> openAddItemDialog(type));
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        header.add(mealTitle, mealInfo, addBtn);
        section.add(header);

        if (!items.isEmpty()) {
            Div itemsDiv = new Div();
            itemsDiv.getStyle().set("margin-top", "8px");
            for (DietMealItem item : items) {
                itemsDiv.add(buildItemRow(item, meal));
            }
            section.add(itemsDiv);
        }

        return section;
    }

    private HorizontalLayout buildItemRow(DietMealItem item, DietMeal meal) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);
        row.getStyle().set("border-top", "1px solid var(--lumo-contrast-5pct)").set("padding-top", "6px").set("margin-top", "4px");

        Span name = new Span(item.getDisplayName());
        name.getStyle().set("flex-grow", "1");

        Span macros = new Span(
                fmt(item.getEffectiveKcal()) + " kcal  |  " +
                fmt(item.getEffectiveProtein()) + "g P  |  " +
                fmt(item.getEffectiveFat()) + "g F  |  " +
                fmt(item.getEffectiveCarbs()) + "g C"
        );
        macros.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--lumo-font-size-s)");

        Button remove = new Button(VaadinIcon.TRASH.create(), e -> {
            dietService.removeItem(currentDay, item);
            currentDay = dietService.getOrCreateDay(selectedDate);
            refresh();
        });
        remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        row.add(name, macros, remove);
        return row;
    }

    private void openAddItemDialog(DietMeal.MealType mealType) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add to " + mealType.getDisplayName());
        dialog.setWidth("500px");

        Tab quickTab = new Tab("Quick Entry");
        Tab ingredientTab = new Tab("From Ingredient");
        Tabs tabs = new Tabs(quickTab, ingredientTab);

        Div quickContent = buildQuickEntryForm(dialog, mealType);
        Div ingredientContent = buildIngredientForm(dialog, mealType);
        ingredientContent.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            quickContent.setVisible(tabs.getSelectedTab() == quickTab);
            ingredientContent.setVisible(tabs.getSelectedTab() == ingredientTab);
        });

        VerticalLayout content = new VerticalLayout(tabs, quickContent, ingredientContent);
        content.setPadding(false);
        content.setSpacing(false);

        dialog.add(content);
        dialog.open();
    }

    private Div buildQuickEntryForm(Dialog dialog, DietMeal.MealType mealType) {
        Div div = new Div();
        FormLayout form = new FormLayout();

        TextField descField = new TextField("Description");
        descField.setWidthFull();
        NumberField kcalField = new NumberField("Calories (kcal)");
        NumberField proteinField = new NumberField("Protein (g)");
        NumberField fatField = new NumberField("Fat (g)");
        NumberField carbsField = new NumberField("Carbs (g)");

        form.add(descField, kcalField, proteinField, fatField, carbsField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("400px", 2));

        Button save = new Button("Add", e -> {
            if (kcalField.getValue() == null && proteinField.getValue() == null) {
                Notification.show("Enter at least calories or protein.");
                return;
            }
            DietMealItem item = new DietMealItem();
            item.setDescription(descField.getValue());
            item.setKcal(kcalField.getValue());
            item.setProtein(proteinField.getValue());
            item.setFat(fatField.getValue());
            item.setCarbs(carbsField.getValue());

            DietMeal meal = dietService.getOrCreateMeal(currentDay, mealType);
            currentDay = dietService.getOrCreateDay(selectedDate);
            DietMeal freshMeal = currentDay.getMeals().stream()
                    .filter(m -> m.getMealType() == mealType && !Boolean.TRUE.equals(m.isDeleted()))
                    .findFirst().orElseThrow();
            dietService.addItemToMeal(currentDay, freshMeal, item);
            currentDay = dietService.getOrCreateDay(selectedDate);
            refresh();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        div.add(form, buttons);
        return div;
    }

    private Div buildIngredientForm(Dialog dialog, DietMeal.MealType mealType) {
        Div div = new Div();
        FormLayout form = new FormLayout();

        ComboBox<Ingredient> ingredientCombo = new ComboBox<>("Ingredient");
        ingredientCombo.setWidthFull();
        ingredientCombo.setItemLabelGenerator(Ingredient::getName);
        ingredientCombo.setItems(query -> {
            String filter = query.getFilter().orElse("");
            List<Ingredient> results = ingredientService.searchByText(filter, query.getOffset(), query.getLimit());
            return results.stream();
        });

        NumberField amountField = new NumberField("Amount (g)");
        amountField.setMin(0.1);

        Div macroPreview = new Div();
        macroPreview.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--bervan-text-secondary)").set("margin-top", "4px");

        ingredientCombo.addValueChangeListener(e -> updateMacroPreview(macroPreview, e.getValue(), amountField.getValue()));
        amountField.addValueChangeListener(e -> updateMacroPreview(macroPreview, ingredientCombo.getValue(), e.getValue()));

        form.add(ingredientCombo, amountField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Add", e -> {
            Ingredient selected = ingredientCombo.getValue();
            Double amount = amountField.getValue();
            if (selected == null || amount == null) {
                Notification.show("Select ingredient and enter amount.");
                return;
            }
            DietMealItem item = new DietMealItem();
            item.setIngredient(selected);
            item.setAmountGrams(amount);

            dietService.getOrCreateMeal(currentDay, mealType);
            currentDay = dietService.getOrCreateDay(selectedDate);
            DietMeal freshMeal = currentDay.getMeals().stream()
                    .filter(m -> m.getMealType() == mealType && !Boolean.TRUE.equals(m.isDeleted()))
                    .findFirst().orElseThrow();
            dietService.addItemToMeal(currentDay, freshMeal, item);
            currentDay = dietService.getOrCreateDay(selectedDate);
            refresh();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);

        div.add(form, macroPreview, buttons);
        return div;
    }

    private void updateMacroPreview(Div preview, Ingredient ingredient, Double grams) {
        if (ingredient == null || grams == null || !ingredient.hasMacros()) {
            preview.setText("");
            return;
        }
        double kcal = ingredient.getKcalPer100g() * grams / 100.0;
        double protein = ingredient.getProteinPer100g() != null ? ingredient.getProteinPer100g() * grams / 100.0 : 0;
        double fat = ingredient.getFatPer100g() != null ? ingredient.getFatPer100g() * grams / 100.0 : 0;
        double carbs = ingredient.getCarbsPer100g() != null ? ingredient.getCarbsPer100g() * grams / 100.0 : 0;
        preview.setText(String.format("%.0f kcal  |  %.1fg protein  |  %.1fg fat  |  %.1fg carbs", kcal, protein, fat, carbs));
    }

    private void openGoalsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Daily Goals");

        FormLayout form = new FormLayout();
        NumberField kcalField = new NumberField("Target Calories (kcal)");
        kcalField.setValue(currentDay.getTargetKcal() != null ? currentDay.getTargetKcal().doubleValue() : null);

        NumberField proteinField = new NumberField("Target Protein (g)");
        proteinField.setValue(currentDay.getTargetProtein() != null ? currentDay.getTargetProtein().doubleValue() : null);

        NumberField activityField = new NumberField("Activity Calories Burned");
        activityField.setValue(currentDay.getActivityKcal() != null ? currentDay.getActivityKcal().doubleValue() : 0.0);

        form.add(kcalField, proteinField, activityField);

        Button save = new Button("Save", e -> {
            dietService.updateDayTargets(
                    currentDay,
                    kcalField.getValue() != null ? kcalField.getValue().intValue() : null,
                    proteinField.getValue() != null ? proteinField.getValue().intValue() : null,
                    activityField.getValue() != null ? activityField.getValue().intValue() : 0
            );
            currentDay = dietService.getOrCreateDay(selectedDate);
            refresh();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.add(form, new HorizontalLayout(save, cancel));
        dialog.open();
    }

    private String fmt(double val) {
        if (val == Math.floor(val)) return String.valueOf((int) val);
        return String.format("%.1f", val);
    }
}
