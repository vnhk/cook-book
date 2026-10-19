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
        recipe.setDeleted(false);

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
        recipe.setRequiredEquipment(data.getRequiredEquipment());
        recipe.setSourceUrl(data.getSourceUrl());
        recipe.setMainImageUrl(data.getMainImageUrl());

        Map<String, String> unifiedTags = new LinkedHashMap<>();
        if (data.getTags() != null && !data.getTags().isBlank()) {
            Arrays.stream(data.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(t -> unifiedTags.putIfAbsent(t.toLowerCase(), t));
        }

        Set<RecipeIngredient> recipeIngredients = new LinkedHashSet<>();

        if (data.getIngredientLines() != null) {
            for (ScrapedRecipeData.ScrapedIngredientLine line : data.getIngredientLines()) {
                String ingredientText = line.getIngredientText();
                if (ingredientText == null || ingredientText.isBlank()) {
                    continue;
                }

                Ingredient ingredient = normalizationEngine.normalize(ingredientText)
                        .orElseGet(() -> ingredientService.findOrCreateByName(
                                ingredientText, line.getIngredientCategory()));

                CulinaryUnit unit = unitConversionEngine.parseUnit(line.getUnitText());

                RecipeIngredient ri = new RecipeIngredient();
                ri.setId(UUID.randomUUID());
                ri.setRecipe(recipe);
                ri.setIngredient(ingredient);
                ri.setQuantity(line.getQuantity());
                ri.setUnit(unit);
                ri.setOptional(Boolean.TRUE.equals(line.getOptional()));
                ri.setCategory(line.getCategory());
                ri.setOriginalText(formatUnifiedIngredientLine(ingredient.getName(), line.getQuantity(), unit, line.getOriginalText()));
                ri.setDeleted(false);

                recipeIngredients.add(ri);

                if (!Boolean.TRUE.equals(ri.getOptional()) && ingredient.getName() != null) {
                    String ingTag = ingredient.getName().trim();
                    if (!ingTag.isEmpty()) {
                        unifiedTags.putIfAbsent(ingTag.toLowerCase(), ingTag);
                    }
                }
            }
        }

        recipe.setTags(new ArrayList<>(unifiedTags.values()));
        recipe.setRecipeIngredients(recipeIngredients);
        return recipeService.save(recipe);
    }

    private String formatUnifiedIngredientLine(String canonicalName, Double quantity, CulinaryUnit unit, String rawOriginalText) {
        if (rawOriginalText != null && !rawOriginalText.isBlank()
                && rawOriginalText.toLowerCase().startsWith(canonicalName.toLowerCase() + " -")) {
            return canonicalName + rawOriginalText.substring(canonicalName.length());
        }
        if (quantity != null) {
            String qtyStr = quantity == Math.floor(quantity)
                    ? String.valueOf(quantity.intValue())
                    : String.valueOf(quantity);
            String unitStr = unit != null ? unit.getDisplayName() : "";
            return (canonicalName + " - " + qtyStr + (unitStr.isEmpty() ? "" : " " + unitStr)).trim();
        }
        return canonicalName;
    }
}
