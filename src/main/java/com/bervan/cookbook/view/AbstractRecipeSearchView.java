package com.bervan.cookbook.view;

import com.bervan.common.component.BervanButton;

import com.bervan.common.view.AbstractPageView;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.service.RecipeMatchingEngine;
import com.bervan.cookbook.service.RecipeMatchingEngine.RecipeMatchResult;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.ArrayList;
import java.util.List;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractRecipeSearchView extends AbstractPageView {
    public static final String ROUTE_NAME = "/cook-book/search";

    private final RecipeMatchingEngine matchingEngine;
    private final CookBookPageLayout pageLayout = new CookBookPageLayout(ROUTE_NAME);
    private final List<String> fridgeIngredients = new ArrayList<>();
    private final VerticalLayout resultsContainer = new VerticalLayout();
    private FlexLayout chipsContainer;

    public AbstractRecipeSearchView(RecipeMatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
        add(pageLayout);
        setWidthFull();
        buildSearchPanel();
        add(resultsContainer);
    }

    private void buildSearchPanel() {
        Div section = new Div();
        section.addClassName("bervan-glass-card");
        section.addClassName("pm-section");
        section.getStyle().set("width", "100%");

        H3 title = new H3("Search by Fridge Contents");
        section.add(title);

        // Ingredient input
        HorizontalLayout inputRow = new HorizontalLayout();
        inputRow.setWidthFull();
        inputRow.setAlignItems(FlexComponent.Alignment.END);

        TextField ingredientInput = new TextField("Add ingredient");
        ingredientInput.setWidthFull();
        ingredientInput.setPlaceholder("Type ingredient name and press Enter");
        ingredientInput.addKeyPressListener(Key.ENTER, e -> {
            String value = ingredientInput.getValue();
            if (value != null && !value.isBlank()) {
                fridgeIngredients.add(value.trim());
                ingredientInput.clear();
                refreshChips();
            }
        });

        inputRow.add(ingredientInput);
        section.add(inputRow);

        // Chips display
        chipsContainer = new FlexLayout();
        chipsContainer.getStyle().set("flex-wrap", "wrap").set("gap", "6px")
                .set("margin-top", "var(--bervan-spacing-sm)");
        section.add(chipsContainer);

        // Threshold + search button
        HorizontalLayout controlRow = new HorizontalLayout();
        controlRow.setAlignItems(FlexComponent.Alignment.END);
        controlRow.getStyle().set("margin-top", "var(--bervan-spacing-md)");

        IntegerField thresholdField = new IntegerField("Min coverage %");
        thresholdField.setValue(50);
        thresholdField.setMin(0);
        thresholdField.setMax(100);
        thresholdField.setStep(10);

        BervanButton searchBtn = new BervanButton("Search Recipes");
        searchBtn.addClickListener(e -> {
            if (fridgeIngredients.isEmpty()) {
                showErrorNotification("Add at least one ingredient");
                return;
            }
            int threshold = thresholdField.getValue() != null ? thresholdField.getValue() : 50;
            performSearch(threshold);
        });

        BervanButton clearBtn = new BervanButton("Clear");
        clearBtn.addClickListener(e -> {
            fridgeIngredients.clear();
            refreshChips();
            resultsContainer.removeAll();
        });

        controlRow.add(thresholdField, searchBtn, clearBtn);
        section.add(controlRow);

        add(section);
    }

    private void refreshChips() {
        chipsContainer.removeAll();
        for (int i = 0; i < fridgeIngredients.size(); i++) {
            final int idx = i;
            String name = fridgeIngredients.get(i);
            Span chip = new Span(name + " ×");
            chip.addClassName("cb-ingredient-chip");
            chip.addClassName("cb-ingredient-matched");
            chip.getStyle().set("cursor", "pointer");
            chip.addClickListener(e -> {
                fridgeIngredients.remove(idx);
                refreshChips();
            });
            chipsContainer.add(chip);
        }
    }

    private void performSearch(int minCoveragePercent) {
        resultsContainer.removeAll();

        List<RecipeMatchResult> results = matchingEngine.findMatchingRecipes(
                fridgeIngredients, minCoveragePercent);

        if (results.isEmpty()) {
            resultsContainer.add(new Span("No matching recipes found."));
            return;
        }

        Div resultsSection = new Div();
        resultsSection.addClassName("bervan-glass-card");
        resultsSection.addClassName("pm-section");
        resultsSection.getStyle().set("width", "100%");

        H3 title = new H3("Results (" + results.size() + " recipes found)");
        resultsSection.add(title);

        Grid<RecipeMatchResult> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.setItems(results);

        grid.addColumn(new ComponentRenderer<>(r -> {
            Anchor link = new Anchor(AbstractRecipeDetailView.ROUTE_NAME + r.getRecipe().getId(),
                    r.getRecipe().getName());
            return link;
        })).setHeader("Recipe").setFlexGrow(2);

        grid.addColumn(RecipeMatchResult::getMatchCount).setHeader("Matches").setWidth("80px");

        grid.addColumn(r -> String.format("%.0f%%", r.getCoveragePercent()))
                .setHeader("Coverage").setWidth("90px");

        grid.addColumn(new ComponentRenderer<>(r ->
                CookBookUIHelper.createRatingStars(r.getRecipe().getAverageRating())
        )).setHeader("Rating").setWidth("150px");

        grid.addColumn(new ComponentRenderer<>(r -> {
            FlexLayout chips = new FlexLayout();
            chips.getStyle().set("flex-wrap", "wrap").set("gap", "2px");
            for (Ingredient ing : r.getMatchedIngredients()) {
                chips.add(CookBookUIHelper.createIngredientChip(ing.getName(), true));
            }
            for (Ingredient ing : r.getMissingIngredients()) {
                chips.add(CookBookUIHelper.createIngredientChip(ing.getName(), false));
            }
            return chips;
        })).setHeader("Ingredients").setFlexGrow(3);

        resultsSection.add(grid);
        resultsContainer.add(resultsSection);
    }
}
