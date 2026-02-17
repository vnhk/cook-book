package com.bervan.cookbook.service;

import com.bervan.cookbook.model.*;
import com.bervan.cookbook.scraper.RecipeScraperStrategy;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecipeImportService {
    private final List<RecipeScraperStrategy> scraperStrategies;
    private final IngredientNormalizationEngine normalizationEngine;
    private final UnitConversionEngine unitConversionEngine;
    private final RecipeService recipeService;
    private final IngredientService ingredientService;

    public RecipeImportService(List<RecipeScraperStrategy> scraperStrategies,
                               IngredientNormalizationEngine normalizationEngine,
                               UnitConversionEngine unitConversionEngine,
                               RecipeService recipeService,
                               IngredientService ingredientService) {
        this.scraperStrategies = scraperStrategies != null ? scraperStrategies : Collections.emptyList();
        this.normalizationEngine = normalizationEngine;
        this.unitConversionEngine = unitConversionEngine;
        this.recipeService = recipeService;
        this.ingredientService = ingredientService;
    }

    public List<String> getAvailableScraperNames() {
        return scraperStrategies.stream()
                .map(RecipeScraperStrategy::getName)
                .toList();
    }

    public RecipeScraperStrategy findStrategyByName(String name) {
        return scraperStrategies.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No scraper found with name: " + name));
    }

    public ScrapedRecipeData scrapePreview(String scraperName, String html) {
        RecipeScraperStrategy strategy = findStrategyByName(scraperName);
        return strategy.scrape(html);
    }

    public Recipe importFromScraped(ScrapedRecipeData data) {
        Recipe recipe = new Recipe();
        recipe.setId(UUID.randomUUID());
        recipe.setName(data.getName());
        recipe.setDescription(data.getDescription());

        // Sanitize HTML instruction - allow images and basic formatting
        if (data.getInstructionHtml() != null) {
            Safelist safelist = Safelist.relaxed().addTags("img")
                    .addAttributes("img", "src", "alt", "width", "height");
            recipe.setInstruction(Jsoup.clean(data.getInstructionHtml(), safelist));
        }

        recipe.setPrepTime(data.getPrepTime());
        recipe.setCookTime(data.getCookTime());
        recipe.setServings(data.getServings());
        recipe.setTotalCalories(data.getTotalCalories());
        recipe.setAverageRating(data.getAverageRating());
        recipe.setRatingCount(data.getRatingCount());
        if (data.getTags() != null && !data.getTags().isBlank()) {
            recipe.setTags(Arrays.stream(data.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
        recipe.setRequiredEquipment(data.getRequiredEquipment());
        recipe.setSourceUrl(data.getSourceUrl());
        recipe.setMainImageUrl(data.getMainImageUrl());

        Set<RecipeIngredient> recipeIngredients = new HashSet<>();

        if (data.getIngredientLines() != null) {
            for (ScrapedRecipeData.ScrapedIngredientLine line : data.getIngredientLines()) {
                RecipeIngredient ri = new RecipeIngredient();
                ri.setId(UUID.randomUUID());
                ri.setRecipe(recipe);
                ri.setOriginalText(line.getOriginalText());
                ri.setQuantity(line.getQuantity());
                ri.setOptional(false);
                ri.setCategory(line.getCategory());

                // Parse unit
                CulinaryUnit unit = unitConversionEngine.parseUnit(line.getUnitText());
                ri.setUnit(unit);

                // Normalize ingredient
                String ingredientText = line.getIngredientText();
                if (ingredientText != null && !ingredientText.isBlank()) {
                    Ingredient ingredient = normalizationEngine.normalize(ingredientText)
                            .orElseGet(() -> ingredientService.findOrCreateByName(ingredientText));
                    ri.setIngredient(ingredient);
                    recipeIngredients.add(ri);
                }
            }
        }

        recipe.setRecipeIngredients(recipeIngredients);
        return recipeService.save(recipe);
    }
}
