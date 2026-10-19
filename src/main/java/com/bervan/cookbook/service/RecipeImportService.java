package com.bervan.cookbook.service;

import com.bervan.cookbook.model.*;
import com.bervan.cookbook.scraper.RecipeScraperStrategy;
import com.bervan.logging.JsonLogger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecipeImportService {
    private static final JsonLogger log = JsonLogger.getLogger(RecipeImportService.class, "cook-book");

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
        String rawName = data.getName() != null && data.getName().trim().length() >= 2
                ? data.getName().trim()
                : "Imported Recipe";
        recipe.setName(truncate(rawName, 300));
        recipe.setDescription(data.getDescription());
        recipe.setDeleted(false);
        recipe.setModificationDate(LocalDateTime.now());

        // Sanitize HTML instruction - allow images, h4 headers, and basic formatting
        if (data.getInstructionHtml() != null) {
            Safelist safelist = Safelist.relaxed().addTags("img", "h4")
                    .addAttributes("img", "src", "alt", "width", "height");
            recipe.setInstruction(Jsoup.clean(data.getInstructionHtml(), safelist));
        }

        recipe.setPrepTime(data.getPrepTime());
        recipe.setCookTime(data.getCookTime());
        recipe.setServings(data.getServings());
        recipe.setTotalCalories(data.getTotalCalories());
        recipe.setAverageRating(data.getAverageRating());
        recipe.setRatingCount(data.getRatingCount());
        recipe.setRequiredEquipment(truncate(data.getRequiredEquipment(), 500));
        recipe.setSourceUrl(truncate(data.getSourceUrl(), 300));
        recipe.setMainImageUrl(truncate(data.getMainImageUrl(), 1000));

        Map<String, String> unifiedTags = new LinkedHashMap<>();
        if (data.getTags() != null && !data.getTags().isBlank()) {
            Arrays.stream(data.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> truncate(s, 250))
                    .forEach(t -> unifiedTags.putIfAbsent(t.toLowerCase(), t));
        }

        Set<RecipeIngredient> recipeIngredients = new LinkedHashSet<>();

        if (data.getIngredientLines() != null) {
            for (ScrapedRecipeData.ScrapedIngredientLine line : data.getIngredientLines()) {
                if (line == null) {
                    continue;
                }
                String ingredientText = line.getIngredientText();
                if (ingredientText == null || ingredientText.isBlank()) {
                    continue;
                }

                try {
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
                    ri.setCategory(truncate(line.getCategory(), 100));
                    ri.setOriginalText(truncate(
                            formatUnifiedIngredientLine(ingredient.getName(), line.getQuantity(), unit, line.getOriginalText()),
                            300));
                    ri.setModificationDate(LocalDateTime.now());
                    ri.setDeleted(false);
                    recipe.getOwners().forEach(ri::addOwner);

                    recipeIngredients.add(ri);

                    if (!Boolean.TRUE.equals(ri.getOptional()) && ingredient.getName() != null) {
                        String ingTag = truncate(ingredient.getName().trim(), 250);
                        if (!ingTag.isEmpty()) {
                            unifiedTags.putIfAbsent(ingTag.toLowerCase(), ingTag);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process scraped ingredient line '" + ingredientText + "': " + e.getMessage(), e);
                }
            }
        }

        recipe.setTags(new ArrayList<>(unifiedTags.values()));
        recipe.setRecipeIngredients(recipeIngredients);
        log.info("Saving imported recipe '{}' with {} ingredients and {} tags",
                recipe.getName(), recipeIngredients.size(), recipe.getTags().size());
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

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
