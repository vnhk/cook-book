package com.bervan.cookbook.view;

import com.bervan.common.component.BervanComboBox;
import com.bervan.common.view.AbstractPageView;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractDietView extends AbstractPageView {

    public static final String ROUTE_NAME = "/cook-book/diet";

    private static final Map<String, String> ACTIVITY_LEVELS = new LinkedHashMap<>();
    static {
        ACTIVITY_LEVELS.put("SEDENTARY",   "Sedentary (desk job, no exercise)");
        ACTIVITY_LEVELS.put("LIGHT",       "Light (1-3x/week)");
        ACTIVITY_LEVELS.put("MODERATE",    "Moderate (3-5x/week)");
        ACTIVITY_LEVELS.put("ACTIVE",      "Active (6-7x/week)");
        ACTIVITY_LEVELS.put("VERY_ACTIVE", "Very Active (athlete / physical job)");
    }

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

        Button configButton = new Button("Set Data", VaadinIcon.COG.create(), e -> openDataDialog());
        configButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD.create(), e -> openExportDialog());
        exportButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        bar.add(prev, datePicker, next, today, configButton, exportButton);
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
        double fiber = dietService.totalFiber(currentDay);

        int targetKcal = currentDay.getTargetKcal() != null ? currentDay.getTargetKcal() : 0;
        int targetProtein = currentDay.getTargetProtein() != null ? currentDay.getTargetProtein() : 0;
        int targetCarbs = currentDay.getTargetCarbs() != null ? currentDay.getTargetCarbs() : 0;
        int targetFat = currentDay.getTargetFat() != null ? currentDay.getTargetFat() : 0;
        int targetFiber = currentDay.getTargetFiber() != null ? currentDay.getTargetFiber() : 0;
        int activity = currentDay.getActivityKcal() != null ? currentDay.getActivityKcal() : 0;
        int activityPct = currentDay.getActivityKcalPercent() != null ? currentDay.getActivityKcalPercent() : 100;
        double effectiveActivity = activity * activityPct / 100.0;

        double remainingKcal = targetKcal + effectiveActivity - consumed;
        double remainingProtein = targetProtein - protein;
        double remainingFat = targetFat - fat;
        double remainingCarbs = targetCarbs - carbs;
        double remainingFiber = targetFiber - fiber;

        String dateLabel = currentDay.getDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(Alignment.CENTER);
        titleRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        titleRow.getStyle().set("margin-bottom", "12px");

        H3 title = new H3(dateLabel);
        title.getStyle().set("margin", "0");

        HorizontalLayout profileChips = new HorizontalLayout();
        profileChips.setSpacing(true);
        profileChips.setAlignItems(Alignment.CENTER);

        if ("M".equals(currentDay.getGender())) {
            profileChips.add(buildProfileChip("♂", null));
        } else if ("F".equals(currentDay.getGender())) {
            profileChips.add(buildProfileChip("♀", null));
        }
        if (currentDay.getAge() != null)
            profileChips.add(buildProfileChip(currentDay.getAge() + "y", null));
        if (currentDay.getHeightCm() != null)
            profileChips.add(buildProfileChip(currentDay.getHeightCm() + " cm", null));
        if (currentDay.getWeightKg() != null)
            profileChips.add(buildProfileChip(currentDay.getWeightKg() + " kg", null));
        if (currentDay.getEstimatedDailyKcal() != null)
            profileChips.add(buildProfileChip("TDEE: " + currentDay.getEstimatedDailyKcal() + " kcal", null));

        if (profileChips.getComponentCount() > 0) {
            titleRow.add(title, profileChips);
        } else {
            titleRow.add(title);
        }

        HorizontalLayout macroRow = new HorizontalLayout();
        macroRow.setSpacing(true);
        macroRow.setWidthFull();
        macroRow.add(
                buildMacroTile("Calories", fmt(consumed), "kcal", targetKcal > 0 ? "/ " + targetKcal : "", false),
                buildMacroTile("Protein", fmt(protein) + "g", "", targetProtein > 0 ? "/ " + targetProtein + "g" : "", false),
                buildMacroTile("Fat", fmt(fat) + "g", "", targetFat > 0 ? "/ " + targetFat + "g" : "", false),
                buildMacroTile("Carbs", fmt(carbs) + "g", "", targetCarbs > 0 ? "/ " + targetCarbs + "g" : "", false),
                buildMacroTile("Fiber", fmt(fiber) + "g", "", targetFiber > 0 ? "/ " + targetFiber + "g" : "", false)
        );

        HorizontalLayout remainingRow = new HorizontalLayout();
        remainingRow.setSpacing(true);
        remainingRow.setWidthFull();
        remainingRow.getStyle().set("margin-top", "8px");
        remainingRow.add(
                buildMacroTile("Kcal left", fmt(remainingKcal), "kcal", activity > 0 ? "(+" + fmt(effectiveActivity) + " act. " + activityPct + "%)" : "", remainingKcal < 0),
                buildMacroTile("Protein left", fmt(remainingProtein) + "g", "", "", remainingProtein < 0),
                buildMacroTile("Fat left", fmt(remainingFat) + "g", "", "", remainingFat < 0),
                buildMacroTile("Carbs left", fmt(remainingCarbs) + "g", "", "", remainingCarbs < 0),
                buildMacroTile("Fiber left", fmt(remainingFiber) + "g", "", "", remainingFiber < 0)
        );

        card.add(titleRow, macroRow, remainingRow);

        if (currentDay.getNotes() != null && !currentDay.getNotes().isBlank()) {
            Div notesDiv = new Div();
            notesDiv.getStyle()
                    .set("margin-top", "10px")
                    .set("padding", "8px 12px")
                    .set("background", "var(--bervan-surface-2)")
                    .set("border", "1px solid var(--bervan-border-color)")
                    .set("border-radius", "6px")
                    .set("font-size", "var(--bervan-font-size-sm)")
                    .set("color", "var(--bervan-text-secondary)")
                    .set("white-space", "pre-wrap");
            notesDiv.setText(currentDay.getNotes());
            card.add(notesDiv);
        }

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

    private Span buildProfileChip(String text, String color) {
        Span chip = new Span(text);
        chip.getStyle()
                .set("font-size", "var(--bervan-font-size-lg)")
                .set("font-weight", "600")
                .set("color", color != null ? color : "var(--bervan-text-primary)")
                .set("background", "var(--bervan-surface-2)")
                .set("border", "1px solid var(--bervan-border-color)")
                .set("border-radius", "6px")
                .set("padding", "4px 10px");
        return chip;
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
        double mealFat = items.stream().mapToDouble(DietMealItem::getEffectiveFat).sum();
        double mealCarbs = items.stream().mapToDouble(DietMealItem::getEffectiveCarbs).sum();
        double mealFiber = items.stream().mapToDouble(DietMealItem::getEffectiveFiber).sum();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H4 mealTitle = new H4(type.getDisplayName());
        mealTitle.getStyle().set("margin", "0");

        HorizontalLayout mealInfo = new HorizontalLayout();
        mealInfo.setSpacing(false);
        mealInfo.getStyle().set("gap", "10px");
        Span kcalSpan = new Span(fmt(mealKcal) + " kcal");
        kcalSpan.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--bervan-font-size-sm)");
        Span proteinSpan = new Span(fmt(mealProtein) + "g P");
        proteinSpan.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--bervan-font-size-sm)");
        Span fatSpan = new Span(fmt(mealFat) + "g F");
        fatSpan.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--bervan-font-size-sm)");
        Span carbsSpan = new Span(fmt(mealCarbs) + "g C");
        carbsSpan.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--bervan-font-size-sm)");
        Span fiberSpan = new Span(fmt(mealFiber) + "g Fi");
        fiberSpan.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--bervan-font-size-sm)");
        mealInfo.add(kcalSpan, proteinSpan, fatSpan, carbsSpan, fiberSpan);

        Button addBtn = new Button("Add", VaadinIcon.PLUS.create(), e -> openAddItemDialog(type));
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button copyBtn = new Button("Copy", VaadinIcon.COPY.create(), e -> openCopyMealDialog(type));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(copyBtn, addBtn);
        buttons.setSpacing(true);

        header.add(mealTitle, mealInfo, buttons);
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
        row.getStyle().set("border-top", "1px solid var(--bervan-border-color)").set("padding-top", "6px").set("margin-top", "4px");

        Span name = new Span(item.getDisplayName());
        name.getStyle().set("flex-grow", "1");

        Span macros = new Span(
                fmt(item.getEffectiveKcal()) + " kcal  |  " +
                fmt(item.getEffectiveProtein()) + "g P  |  " +
                fmt(item.getEffectiveFat()) + "g F  |  " +
                fmt(item.getEffectiveCarbs()) + "g C  |  " +
                fmt(item.getEffectiveFiber()) + "g Fi"
        );
        macros.getStyle().set("color", "var(--bervan-text-secondary)").set("font-size", "var(--bervan-font-size-sm)");

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

        Tab ingredientTab = new Tab("From Ingredient");
        Tab quickTab = new Tab("Quick Entry");
        Tabs tabs = new Tabs(ingredientTab, quickTab);

        Div ingredientContent = buildIngredientForm(dialog, mealType);
        Div quickContent = buildQuickEntryForm(dialog, mealType);
        quickContent.setVisible(false);

        tabs.addSelectedChangeListener(e -> {
            ingredientContent.setVisible(tabs.getSelectedTab() == ingredientTab);
            quickContent.setVisible(tabs.getSelectedTab() == quickTab);
        });

        VerticalLayout content = new VerticalLayout(tabs, ingredientContent, quickContent);
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
        NumberField fiberField = new NumberField("Fiber (g)");

        form.add(descField, kcalField, proteinField, fatField, carbsField, fiberField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("400px", 2));

        Button save = new Button("Add", e -> {
            if (kcalField.getValue() == null && proteinField.getValue() == null) {
                showErrorNotification("Enter at least calories or protein.");
                return;
            }
            DietMealItem item = new DietMealItem();
            item.setDescription(descField.getValue());
            item.setKcal(kcalField.getValue());
            item.setProtein(proteinField.getValue());
            item.setFat(fatField.getValue());
            item.setCarbs(carbsField.getValue());
            item.setFiber(fiberField.getValue());

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
        macroPreview.getStyle().set("font-size", "var(--bervan-font-size-sm)").set("color", "var(--bervan-text-secondary)").set("margin-top", "4px");

        ingredientCombo.addValueChangeListener(e -> updateMacroPreview(macroPreview, e.getValue(), amountField.getValue()));
        amountField.addValueChangeListener(e -> updateMacroPreview(macroPreview, ingredientCombo.getValue(), e.getValue()));

        form.add(ingredientCombo, amountField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Add", e -> {
            Ingredient selected = ingredientCombo.getValue();
            Double amount = amountField.getValue();
            if (selected == null || amount == null) {
                showErrorNotification("Select ingredient and enter amount.");
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

    private void openCopyMealDialog(DietMeal.MealType targetType) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Copy meal to " + targetType.getDisplayName());
        dialog.setWidth("500px");

        DatePicker sourceDatePicker = new DatePicker("Source date");
        sourceDatePicker.setValue(selectedDate.minusDays(1));

        ComboBox<DietMeal.MealType> sourceMealCombo = new ComboBox<>("Source meal");
        sourceMealCombo.setItems(DietMeal.MealType.values());
        sourceMealCombo.setItemLabelGenerator(DietMeal.MealType::getDisplayName);
        sourceMealCombo.setValue(targetType);
        sourceMealCombo.setWidthFull();

        Div preview = new Div();
        preview.getStyle()
                .set("margin-top", "8px")
                .set("padding", "8px 12px")
                .set("background", "var(--bervan-surface-2)")
                .set("border", "1px solid var(--bervan-border-color)")
                .set("border-radius", "6px")
                .set("min-height", "48px")
                .set("font-size", "var(--bervan-font-size-sm)")
                .set("color", "var(--bervan-text-secondary)");

        Runnable updatePreview = () -> {
            preview.removeAll();
            LocalDate srcDate = sourceDatePicker.getValue();
            DietMeal.MealType srcType = sourceMealCombo.getValue();
            if (srcDate == null || srcType == null) return;
            dietService.findByDate(srcDate).ifPresentOrElse(srcDay -> {
                List<DietMealItem> items = srcDay.getMeals().stream()
                        .filter(m -> m.getMealType() == srcType && !Boolean.TRUE.equals(m.isDeleted()))
                        .findFirst()
                        .map(m -> m.getItems().stream().filter(i -> !Boolean.TRUE.equals(i.isDeleted())).toList())
                        .orElse(List.of());
                if (items.isEmpty()) {
                    preview.add(new Span("No items found."));
                } else {
                    for (DietMealItem item : items) {
                        Div row = new Div();
                        row.getStyle().set("padding", "2px 0");
                        row.setText("• " + item.getDisplayName() + "  —  " +
                                fmt(item.getEffectiveKcal()) + " kcal | " +
                                fmt(item.getEffectiveProtein()) + "g P | " +
                                fmt(item.getEffectiveFat()) + "g F | " +
                                fmt(item.getEffectiveCarbs()) + "g C");
                        preview.add(row);
                    }
                }
            }, () -> preview.add(new Span("No data for this date.")));
        };

        sourceDatePicker.addValueChangeListener(e -> updatePreview.run());
        sourceMealCombo.addValueChangeListener(e -> updatePreview.run());
        updatePreview.run();

        Button confirmBtn = new Button("Copy", e -> {
            LocalDate srcDate = sourceDatePicker.getValue();
            DietMeal.MealType srcType = sourceMealCombo.getValue();
            if (srcDate == null || srcType == null) {
                showErrorNotification("Select date and meal.");
                return;
            }
            dietService.copyMealItems(currentDay, targetType, srcDate, srcType);
            currentDay = dietService.getOrCreateDay(selectedDate);
            refresh();
            dialog.close();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        FormLayout form = new FormLayout(sourceDatePicker, sourceMealCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        dialog.add(new VerticalLayout(form, preview, new HorizontalLayout(confirmBtn, cancelBtn)));
        dialog.open();
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
        double fiber2 = ingredient.getFiberPer100g() != null ? ingredient.getFiberPer100g() * grams / 100.0 : 0;
        preview.setText(String.format("%.0f kcal  |  %.1fg P  |  %.1fg F  |  %.1fg C  |  %.1fg Fi", kcal, protein, fat, carbs, fiber2));
    }

    private void openDataDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Daily Data");
        dialog.setWidth("560px");

        // --- Targets ---
        NumberField kcalField = new NumberField("Target Calories / Goal (kcal)");
        kcalField.setValue(currentDay.getTargetKcal() != null ? currentDay.getTargetKcal().doubleValue() : null);
        NumberField estDailyField = new NumberField("Est. Daily Calories / TDEE (kcal)");
        estDailyField.setValue(currentDay.getEstimatedDailyKcal() != null ? currentDay.getEstimatedDailyKcal().doubleValue() : null);
        NumberField proteinField = new NumberField("Target Protein (g)");
        proteinField.setValue(currentDay.getTargetProtein() != null ? currentDay.getTargetProtein().doubleValue() : null);
        NumberField carbsField = new NumberField("Target Carbs (g)");
        carbsField.setValue(currentDay.getTargetCarbs() != null ? currentDay.getTargetCarbs().doubleValue() : null);
        NumberField fatField = new NumberField("Target Fat (g)");
        fatField.setValue(currentDay.getTargetFat() != null ? currentDay.getTargetFat().doubleValue() : null);
        NumberField fiberField = new NumberField("Target Fiber (g)");
        fiberField.setValue(currentDay.getTargetFiber() != null ? currentDay.getTargetFiber().doubleValue() : null);
        NumberField activityField = new NumberField("Activity Calories Burned");
        activityField.setValue(currentDay.getActivityKcal() != null ? currentDay.getActivityKcal().doubleValue() : 0.0);
        activityField.setWidthFull();

        BervanComboBox<Integer> activityPercentSelect = new BervanComboBox<>();
        activityPercentSelect.setLabel("Accuracy");
        activityPercentSelect.setItems(40, 50, 60, 70, 80, 90, 100);
        activityPercentSelect.setItemLabelGenerator(p -> p + "%");
        activityPercentSelect.setValue(currentDay.getActivityKcalPercent() != null ? currentDay.getActivityKcalPercent() : 100);
        activityPercentSelect.setWidth("100px");

        com.vaadin.flow.component.icon.Icon activityInfoIcon = VaadinIcon.QUESTION_CIRCLE_O.create();
        activityInfoIcon.getStyle()
                .set("color", "var(--bervan-text-secondary)")
                .set("cursor", "help")
                .set("align-self", "flex-end")
                .set("padding-bottom", "10px")
                .set("flex-shrink", "0");
        Tooltip.forComponent(activityInfoIcon).withText(
                "Fitness trackers often overestimate calorie burn. Select what percentage of the reported activity calories should count toward your daily deficit."
        );

        HorizontalLayout activityRow = new HorizontalLayout(activityField, activityPercentSelect, activityInfoIcon);
        activityRow.setWidthFull();
        activityRow.setAlignItems(Alignment.END);
        activityRow.setFlexGrow(1, activityField);

        FormLayout targetsForm = new FormLayout(kcalField, estDailyField, proteinField, carbsField, fatField, fiberField);
        targetsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("300px", 2));

        // --- Profile ---
        NumberField weightField = new NumberField("Weight (kg)");
        weightField.setValue(currentDay.getWeightKg());
        weightField.setMin(0);
        weightField.setStep(0.1);

        NumberField heightField = new NumberField("Height (cm)");
        heightField.setValue(currentDay.getHeightCm() != null ? currentDay.getHeightCm().doubleValue() : null);
        heightField.setMin(100);
        heightField.setMax(250);

        NumberField ageField = new NumberField("Age");
        ageField.setValue(currentDay.getAge() != null ? currentDay.getAge().doubleValue() : null);
        ageField.setMin(10);
        ageField.setMax(120);

        RadioButtonGroup<String> genderGroup = new RadioButtonGroup<>("Gender");
        genderGroup.setItems("M", "F");
        genderGroup.setItemLabelGenerator(g -> "M".equals(g) ? "♂ Male" : "♀ Female");
        if (currentDay.getGender() != null) genderGroup.setValue(currentDay.getGender());

        BervanComboBox<String> activityLevelSelect = new BervanComboBox<>();
        activityLevelSelect.setLabel("Activity level");
        activityLevelSelect.setItems(ACTIVITY_LEVELS.keySet().stream().toList());
        activityLevelSelect.setItemLabelGenerator(k -> ACTIVITY_LEVELS.getOrDefault(k, k));
        activityLevelSelect.setWidthFull();
        if (currentDay.getActivityLevel() != null) activityLevelSelect.setValue(currentDay.getActivityLevel());

        FormLayout profileForm = new FormLayout(weightField, heightField, ageField, genderGroup);
        profileForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("300px", 2));

        // --- Calculate TDEE button ---
        Button calcBtn = new Button("Calculate TDEE →", VaadinIcon.CALC.create(), e -> {
            Double w = weightField.getValue();
            Double h = heightField.getValue();
            Double a = ageField.getValue();
            String g = genderGroup.getValue();
            String lvl = activityLevelSelect.getValue();
            if (w == null || w <= 0) { showErrorNotification("Enter a valid weight."); return; }
            if (h == null || h <= 0) { showErrorNotification("Enter a valid height."); return; }
            if (a == null || a <= 0) { showErrorNotification("Enter a valid age."); return; }
            if (g == null)           { showErrorNotification("Select gender."); return; }
            if (lvl == null)         { showErrorNotification("Select activity level."); return; }
            double bmr = "M".equals(g)
                    ? 10 * w + 6.25 * h - 5 * a + 5
                    : 10 * w + 6.25 * h - 5 * a - 161;
            double multiplier = switch (lvl) {
                case "SEDENTARY"   -> 1.2;
                case "LIGHT"       -> 1.375;
                case "MODERATE"    -> 1.55;
                case "ACTIVE"      -> 1.725;
                case "VERY_ACTIVE" -> 1.9;
                default            -> 1.2;
            };
            int tdee = (int) Math.round(bmr * multiplier);
            estDailyField.setValue((double) tdee);
            showSuccessNotification("TDEE calculated: " + tdee + " kcal");
        });
        calcBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        calcBtn.setWidthFull();

        // --- Notes ---
        TextArea notesField = new TextArea("Notes");
        notesField.setValue(currentDay.getNotes() != null ? currentDay.getNotes() : "");
        notesField.setWidthFull();
        notesField.setMaxLength(1000);
        notesField.setHeight("80px");

        // --- Save ---
        Button save = new Button("Save", e -> {
            dietService.updateDayTargets(
                    currentDay,
                    kcalField.getValue() != null ? kcalField.getValue().intValue() : null,
                    estDailyField.getValue() != null ? estDailyField.getValue().intValue() : null,
                    proteinField.getValue() != null ? proteinField.getValue().intValue() : null,
                    carbsField.getValue() != null ? carbsField.getValue().intValue() : null,
                    fatField.getValue() != null ? fatField.getValue().intValue() : null,
                    fiberField.getValue() != null ? fiberField.getValue().intValue() : null,
                    activityField.getValue() != null ? activityField.getValue().intValue() : 0,
                    activityPercentSelect.getValue(),
                    weightField.getValue(),
                    notesField.getValue().isBlank() ? null : notesField.getValue(),
                    ageField.getValue() != null ? ageField.getValue().intValue() : null,
                    genderGroup.getValue(),
                    heightField.getValue() != null ? heightField.getValue().intValue() : null,
                    activityLevelSelect.getValue()
            );
            currentDay = dietService.getOrCreateDay(selectedDate);
            refresh();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());

        VerticalLayout content = new VerticalLayout(
                targetsForm, activityRow, profileForm, activityLevelSelect, calcBtn, notesField,
                new HorizontalLayout(save, cancel));
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);
        dialog.open();
    }

    private void openExportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Export day — " +
                currentDay.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        dialog.setWidth("600px");

        String text = buildExportText();

        TextArea textArea = new TextArea();
        textArea.setValue(text);
        textArea.setWidthFull();
        textArea.setHeight("400px");
        textArea.setReadOnly(true);

        Button copyBtn = new Button("Copy to clipboard", VaadinIcon.CLIPBOARD.create(), e ->
                copyBtn_click(textArea.getValue()));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button closeBtn = new Button("Close", ev -> dialog.close());

        dialog.add(new VerticalLayout(textArea, new HorizontalLayout(copyBtn, closeBtn)));
        dialog.open();
    }

    private void copyBtn_click(String value) {
        com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                "const text = $0;" +
                "if (navigator.clipboard && navigator.clipboard.writeText) {" +
                "  navigator.clipboard.writeText(text).catch(() => fallback(text));" +
                "} else { fallback(text); }" +
                "function fallback(t) {" +
                "  const ta = document.createElement('textarea');" +
                "  ta.value = t; ta.style.position='fixed'; ta.style.opacity='0';" +
                "  document.body.appendChild(ta); ta.focus(); ta.select();" +
                "  document.execCommand('copy'); document.body.removeChild(ta);" +
                "}", value);
        showSuccessNotification("Copied to clipboard!");
    }

    private String buildExportText() {
        StringBuilder sb = new StringBuilder();
        String date = currentDay.getDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
        sb.append("=== Diet log: ").append(date).append(" ===\n\n");

        // Profile
        boolean hasProfile = currentDay.getGender() != null || currentDay.getAge() != null
                || currentDay.getHeightCm() != null || currentDay.getWeightKg() != null;
        if (hasProfile) {
            sb.append("Profile: ");
            if ("M".equals(currentDay.getGender())) sb.append("Male  ");
            else if ("F".equals(currentDay.getGender())) sb.append("Female  ");
            if (currentDay.getAge() != null) sb.append("Age: ").append(currentDay.getAge()).append("  ");
            if (currentDay.getHeightCm() != null) sb.append("Height: ").append(currentDay.getHeightCm()).append(" cm  ");
            if (currentDay.getWeightKg() != null) sb.append("Weight: ").append(currentDay.getWeightKg()).append(" kg");
            sb.append("\n");
        }
        if (currentDay.getActivityLevel() != null)
            sb.append("Activity: ").append(ACTIVITY_LEVELS.getOrDefault(currentDay.getActivityLevel(), currentDay.getActivityLevel())).append("\n");

        int tKcal = currentDay.getTargetKcal() != null ? currentDay.getTargetKcal() : 0;
        int tProtein = currentDay.getTargetProtein() != null ? currentDay.getTargetProtein() : 0;
        int tCarbs = currentDay.getTargetCarbs() != null ? currentDay.getTargetCarbs() : 0;
        int tFat = currentDay.getTargetFat() != null ? currentDay.getTargetFat() : 0;
        int tFiber = currentDay.getTargetFiber() != null ? currentDay.getTargetFiber() : 0;
        int activity = currentDay.getActivityKcal() != null ? currentDay.getActivityKcal() : 0;
        int activityPercent = currentDay.getActivityKcalPercent() != null ? currentDay.getActivityKcalPercent() : 100;
        int effectiveActivityKcal = (int) Math.round(activity * activityPercent / 100.0);

        if (currentDay.getEstimatedDailyKcal() != null)
            sb.append("Est. Daily Calories (TDEE): ").append(currentDay.getEstimatedDailyKcal()).append(" kcal\n");

        if (tKcal > 0 || tProtein > 0) {
            sb.append("Targets: ");
            if (tKcal > 0) sb.append("kcal=").append(tKcal).append(" ");
            if (tProtein > 0) sb.append("protein=").append(tProtein).append("g ");
            if (tCarbs > 0) sb.append("carbs=").append(tCarbs).append("g ");
            if (tFat > 0) sb.append("fat=").append(tFat).append("g ");
            if (tFiber > 0) sb.append("fiber=").append(tFiber).append("g");
            sb.append("\n");
        }
        if (activity > 0) {
            sb.append("Activity burn: +").append(activity).append(" kcal");
            if (activityPercent < 100)
                sb.append(" (counted ").append(activityPercent).append("% = +").append(effectiveActivityKcal).append(" kcal)");
            sb.append("\n");
        }

        sb.append("\n");

        double totalKcal = 0, totalProtein = 0, totalFat = 0, totalCarbs = 0, totalFiber = 0;

        for (DietMeal.MealType type : DietMeal.MealType.values()) {
            DietMeal meal = currentDay.getMeals().stream()
                    .filter(m -> m.getMealType() == type && !Boolean.TRUE.equals(m.isDeleted()))
                    .findFirst().orElse(null);
            List<DietMealItem> items = meal != null
                    ? meal.getItems().stream().filter(i -> !Boolean.TRUE.equals(i.isDeleted())).toList()
                    : List.of();
            if (items.isEmpty()) continue;

            double mKcal = items.stream().mapToDouble(DietMealItem::getEffectiveKcal).sum();
            double mProtein = items.stream().mapToDouble(DietMealItem::getEffectiveProtein).sum();
            double mFat = items.stream().mapToDouble(DietMealItem::getEffectiveFat).sum();
            double mCarbs = items.stream().mapToDouble(DietMealItem::getEffectiveCarbs).sum();
            double mFiber = items.stream().mapToDouble(DietMealItem::getEffectiveFiber).sum();

            sb.append("--- ").append(type.getDisplayName()).append(" ---\n");
            for (DietMealItem item : items) {
                sb.append("  • ").append(item.getDisplayName());
                sb.append("  (").append(fmt(item.getEffectiveKcal())).append(" kcal");
                if (item.getEffectiveProtein() > 0) sb.append(", ").append(fmt(item.getEffectiveProtein())).append("g P");
                if (item.getEffectiveFat() > 0) sb.append(", ").append(fmt(item.getEffectiveFat())).append("g F");
                if (item.getEffectiveCarbs() > 0) sb.append(", ").append(fmt(item.getEffectiveCarbs())).append("g C");
                if (item.getEffectiveFiber() > 0) sb.append(", ").append(fmt(item.getEffectiveFiber())).append("g Fi");
                sb.append(")\n");
            }
            sb.append("  Total: ").append(fmt(mKcal)).append(" kcal | ")
                    .append(fmt(mProtein)).append("g P | ")
                    .append(fmt(mFat)).append("g F | ")
                    .append(fmt(mCarbs)).append("g C | ")
                    .append(fmt(mFiber)).append("g Fi\n\n");

            totalKcal += mKcal;
            totalProtein += mProtein;
            totalFat += mFat;
            totalCarbs += mCarbs;
            totalFiber += mFiber;
        }

        sb.append("=== TOTAL ===\n");
        sb.append("Calories: ").append(fmt(totalKcal));
        if (tKcal > 0) sb.append(" / ").append(tKcal).append(" (remaining: ").append(fmt(tKcal + effectiveActivityKcal - totalKcal)).append(")");
        sb.append("\n");
        sb.append("Protein:  ").append(fmt(totalProtein)).append("g");
        if (tProtein > 0) sb.append(" / ").append(tProtein).append("g (remaining: ").append(fmt(tProtein - totalProtein)).append("g)");
        sb.append("\n");
        sb.append("Fat:      ").append(fmt(totalFat)).append("g");
        if (tFat > 0) sb.append(" / ").append(tFat).append("g (remaining: ").append(fmt(tFat - totalFat)).append("g)");
        sb.append("\n");
        sb.append("Carbs:    ").append(fmt(totalCarbs)).append("g");
        if (tCarbs > 0) sb.append(" / ").append(tCarbs).append("g (remaining: ").append(fmt(tCarbs - totalCarbs)).append("g)");
        sb.append("\n");
        sb.append("Fiber:    ").append(fmt(totalFiber)).append("g");
        if (tFiber > 0) sb.append(" / ").append(tFiber).append("g (remaining: ").append(fmt(tFiber - totalFiber)).append("g)");
        sb.append("\n");

        if (currentDay.getNotes() != null && !currentDay.getNotes().isBlank())
            sb.append("\nNotes: ").append(currentDay.getNotes()).append("\n");

        return sb.toString();
    }

    private String fmt(double val) {
        if (val == Math.floor(val)) return String.valueOf((int) val);
        return String.format("%.1f", val);
    }
}
