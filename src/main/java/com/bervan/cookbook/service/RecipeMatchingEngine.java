package com.bervan.cookbook.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.model.Recipe;
import com.bervan.cookbook.model.RecipeIngredient;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeMatchingEngine {
    private final RecipeService recipeService;
    private final IngredientNormalizationEngine normalizationEngine;

    public RecipeMatchingEngine(RecipeService recipeService,
                                IngredientNormalizationEngine normalizationEngine) {
        this.recipeService = recipeService;
        this.normalizationEngine = normalizationEngine;
    }

    public List<RecipeMatchResult> findMatchingRecipes(List<String> fridgeIngredients,
                                                        double minCoveragePercent) {
        // 1. Normalize fridge ingredients
        Set<UUID> fridgeIngredientIds = new HashSet<>();
        Map<UUID, Ingredient> fridgeMap = new HashMap<>();
        for (String text : fridgeIngredients) {
            normalizationEngine.normalize(text).ifPresent(ingredient -> {
                fridgeIngredientIds.add(ingredient.getId());
                fridgeMap.put(ingredient.getId(), ingredient);
            });
        }

        if (fridgeIngredientIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Load all recipes
        Set<Recipe> allRecipes = recipeService.load(new SearchRequest(), Pageable.ofSize(10000));
        List<RecipeMatchResult> results = new ArrayList<>();

        for (Recipe recipe : allRecipes) {
            if (Boolean.TRUE.equals(recipe.isDeleted())) continue;

            Set<RecipeIngredient> recipeIngredients = recipe.getRecipeIngredients();
            if (recipeIngredients == null || recipeIngredients.isEmpty()) continue;

            // Filter to required (non-optional) ingredients
            List<RecipeIngredient> required = recipeIngredients.stream()
                    .filter(ri -> !Boolean.TRUE.equals(ri.getOptional()))
                    .filter(ri -> !Boolean.TRUE.equals(ri.isDeleted()))
                    .toList();

            if (required.isEmpty()) continue;

            List<Ingredient> matched = new ArrayList<>();
            List<Ingredient> missing = new ArrayList<>();

            for (RecipeIngredient ri : required) {
                if (fridgeIngredientIds.contains(ri.getIngredient().getId())) {
                    matched.add(ri.getIngredient());
                } else {
                    missing.add(ri.getIngredient());
                }
            }

            int matchCount = matched.size();
            double coveragePercent = (matchCount * 100.0) / required.size();

            if (coveragePercent >= minCoveragePercent && matchCount > 0) {
                RecipeMatchResult result = new RecipeMatchResult();
                result.setRecipe(recipe);
                result.setMatchCount(matchCount);
                result.setCoveragePercent(Math.round(coveragePercent * 10.0) / 10.0);
                result.setMatchedIngredients(matched);
                result.setMissingIngredients(missing);
                results.add(result);
            }
        }

        // 3. Sort: matchCount DESC -> coveragePercent DESC -> averageRating DESC
        results.sort(Comparator
                .comparingInt(RecipeMatchResult::getMatchCount).reversed()
                .thenComparingDouble(RecipeMatchResult::getCoveragePercent).reversed()
                .thenComparing(r -> r.getRecipe().getAverageRating() != null
                        ? r.getRecipe().getAverageRating() : 0.0, Comparator.reverseOrder()));

        return results;
    }

    public static class RecipeMatchResult {
        private Recipe recipe;
        private int matchCount;
        private double coveragePercent;
        private List<Ingredient> matchedIngredients;
        private List<Ingredient> missingIngredients;

        public Recipe getRecipe() {
            return recipe;
        }

        public void setRecipe(Recipe recipe) {
            this.recipe = recipe;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public void setMatchCount(int matchCount) {
            this.matchCount = matchCount;
        }

        public double getCoveragePercent() {
            return coveragePercent;
        }

        public void setCoveragePercent(double coveragePercent) {
            this.coveragePercent = coveragePercent;
        }

        public List<Ingredient> getMatchedIngredients() {
            return matchedIngredients;
        }

        public void setMatchedIngredients(List<Ingredient> matchedIngredients) {
            this.matchedIngredients = matchedIngredients;
        }

        public List<Ingredient> getMissingIngredients() {
            return missingIngredients;
        }

        public void setMissingIngredients(List<Ingredient> missingIngredients) {
            this.missingIngredients = missingIngredients;
        }
    }
}
