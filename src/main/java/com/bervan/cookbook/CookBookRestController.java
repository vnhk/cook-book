package com.bervan.cookbook;

import com.bervan.common.search.SearchRequest;
import com.bervan.cookbook.model.*;
import com.bervan.cookbook.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cook-book")
public class CookBookRestController {

    private final RecipeService recipeService;
    private final IngredientService ingredientService;
    private final ShoppingCartService shoppingCartService;
    private final RecipeMatchingEngine matchingEngine;
    private final RecipeImportService importService;
    private final UnitConversionEngine unitConversionEngine;

    public CookBookRestController(RecipeService recipeService, IngredientService ingredientService,
                                  ShoppingCartService shoppingCartService, RecipeMatchingEngine matchingEngine,
                                  RecipeImportService importService, UnitConversionEngine unitConversionEngine) {
        this.recipeService = recipeService;
        this.ingredientService = ingredientService;
        this.shoppingCartService = shoppingCartService;
        this.matchingEngine = matchingEngine;
        this.importService = importService;
        this.unitConversionEngine = unitConversionEngine;
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    private IngredientDto toIngDto(Ingredient i) {
        return new IngredientDto(i.getId(), i.getName(), i.getIcon(), i.getCategory(),
                i.getKcalPer100g(), i.getProteinPer100g(), i.getFatPer100g(),
                i.getCarbsPer100g(), i.getFiberPer100g());
    }

    private RecipeIngredientDto toRiDto(RecipeIngredient ri) {
        Ingredient ing = ri.getIngredient();
        CulinaryUnit unit = ri.getUnit();
        return new RecipeIngredientDto(
                ri.getId(), ing.getId(), ing.getName(), ing.getIcon(),
                ri.getQuantity(),
                unit != null ? unit.name() : null,
                unit != null ? unit.getDisplayName() : null,
                ri.getOptional(), ri.getCategory(), ri.getOriginalText()
        );
    }

    private RecipeDto toRecipeDto(Recipe r) {
        List<RecipeIngredientDto> ingredients = r.getRecipeIngredients() != null
                ? r.getRecipeIngredients().stream()
                .filter(ri -> !Boolean.TRUE.equals(ri.isDeleted()))
                .sorted(Comparator.comparing(ri -> ri.getIngredient().getName()))
                .map(this::toRiDto).collect(Collectors.toList())
                : Collections.emptyList();
        return new RecipeDto(r.getId(), r.getName(), r.getDescription(), r.getInstruction(),
                r.getPrepTime(), r.getCookTime(), r.getTotalTime(),
                r.getServings(), r.getTotalCalories(), r.getAverageRating(),
                r.getRatingCount(), r.getFavorite(), r.getTags(),
                r.getRequiredEquipment(), r.getMainImageUrl(), r.getSourceUrl(), ingredients);
    }

    private CartItemDto toCartItemDto(ShoppingCartItem i) {
        CulinaryUnit unit = i.getUnit();
        return new CartItemDto(i.getId(), i.getIngredient().getId(), i.getIngredient().getName(),
                i.getQuantity(),
                unit != null ? unit.name() : null,
                unit != null ? unit.getDisplayName() : null,
                i.getPurchased(),
                i.getSourceRecipe() != null ? i.getSourceRecipe().getId() : null,
                i.getSourceRecipe() != null ? i.getSourceRecipe().getName() : null);
    }

    private CartDto toCartDto(ShoppingCart c) {
        List<CartItemDto> items = c.getItems() != null
                ? c.getItems().stream()
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .sorted(Comparator.comparing(i -> i.getIngredient().getName()))
                .map(this::toCartItemDto).collect(Collectors.toList())
                : Collections.emptyList();
        return new CartDto(c.getId(), c.getName(), c.getArchived(), items);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @GetMapping("/recipes")
    public ResponseEntity<Page<RecipeDto>> listRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean favorite
    ) {

        Set<Recipe> all = recipeService.load(new SearchRequest(), Pageable.ofSize(100000));
        List<RecipeDto> dtos = all.stream()
                .filter(r -> search == null || r.getName().toLowerCase().contains(search.toLowerCase()))
                .filter(r -> tag == null || (r.getTags() != null && r.getTags().contains(tag)))
                .filter(r -> favorite == null || Objects.equals(r.getFavorite(), favorite))
                .sorted(Comparator.comparing(Recipe::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toRecipeDto)
                .collect(Collectors.toList());

        int total = dtos.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return ResponseEntity.ok(new PageImpl<>(dtos.subList(from, to), PageRequest.of(page, size), total));
    }

    @GetMapping("/recipes/tags")
    public ResponseEntity<List<String>> tags() {
        return ResponseEntity.ok(recipeService.loadAllTags());
    }

    @GetMapping("/recipes/{id}")
    public ResponseEntity<RecipeDto> getRecipe(@PathVariable UUID id) {
        return recipeService.loadById(id)
                .map(r -> ResponseEntity.ok(toRecipeDto(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/recipes")
    public ResponseEntity<RecipeDto> createRecipe(@RequestBody Map<String, Object> req) {
        Recipe r = new Recipe();
        r.setId(UUID.randomUUID());
        r.setModificationDate(LocalDateTime.now());
        r.setDeleted(false);
        applyRecipeFields(r, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toRecipeDto(recipeService.save(r)));
    }

    @PutMapping("/recipes/{id}")
    public ResponseEntity<RecipeDto> updateRecipe(@PathVariable UUID id, @RequestBody Map<String, Object> req) {
        return recipeService.loadById(id).map(r -> {
            applyRecipeFields(r, req);
            r.setModificationDate(LocalDateTime.now());
            return ResponseEntity.ok(toRecipeDto(recipeService.save(r)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Recipes ─────────────────────────────────────────────────────────────

    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        return recipeService.loadById(id).map(r -> {
            recipeService.delete(r);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/recipes/{id}/toggle-favorite")
    public ResponseEntity<Void> toggleFavorite(@PathVariable UUID id) {
        recipeService.toggleFavorite(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recipes/{id}/rate")
    public ResponseEntity<Void> rateRecipe(@PathVariable UUID id,
                                           @RequestParam int rating,
                                           @RequestParam(required = false) String comment) {
        recipeService.addRating(id, rating, comment);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recipes/{id}/ingredients")
    public ResponseEntity<RecipeIngredientDto> addIngredient(@PathVariable UUID id,
                                                             @RequestBody Map<String, Object> req) {
        return recipeService.loadById(id).map(recipe -> {
            String ingName = (String) req.get("ingredientName");
            UUID ingId = req.containsKey("ingredientId")
                    ? UUID.fromString((String) req.get("ingredientId")) : null;

            Ingredient ingredient;
            if (ingId != null) {
                ingredient = ingredientService.loadById(ingId).orElseGet(() -> ingredientService.findOrCreateByName(ingName));
            } else {
                ingredient = ingredientService.findOrCreateByName(ingName);
            }

            RecipeIngredient ri = new RecipeIngredient();
            ri.setId(UUID.randomUUID());
            ri.setRecipe(recipe);
            ri.setIngredient(ingredient);
            if (req.containsKey("quantity") && req.get("quantity") != null)
                ri.setQuantity(Double.parseDouble(req.get("quantity").toString()));
            if (req.containsKey("unit") && req.get("unit") != null)
                ri.setUnit(CulinaryUnit.valueOf((String) req.get("unit")));
            ri.setOptional(Boolean.TRUE.equals(req.get("optional")));
            ri.setCategory((String) req.get("category"));
            ri.setOriginalText((String) req.get("originalText"));
            ri.setModificationDate(LocalDateTime.now());
            ri.setDeleted(false);
            ri.getOwners().addAll(recipe.getOwners());

            recipe.getRecipeIngredients().add(ri);
            recipeService.save(recipe);
            return ResponseEntity.status(HttpStatus.CREATED).body(toRiDto(ri));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/recipes/{id}/ingredients/{riId}")
    public ResponseEntity<Void> removeIngredient(@PathVariable UUID id, @PathVariable UUID riId) {
        return recipeService.loadById(id).map(recipe -> {
            recipe.getRecipeIngredients().removeIf(ri -> ri.getId().equals(riId));
            recipeService.save(recipe);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/recipes/import-html")
    public ResponseEntity<RecipeDto> importHtml(@RequestBody Map<String, Object> req) {
        try {
            String scraperName = (String) req.get("scraperName");
            String html = (String) req.get("html");
            Recipe imported = importService.importFromScraped(importService.scrapePreview(scraperName, html));
            return ResponseEntity.status(HttpStatus.CREATED).body(toRecipeDto(imported));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/recipes/scrapers")
    public ResponseEntity<List<String>> scrapers() {
        return ResponseEntity.ok(importService.getAvailableScraperNames());
    }

    private void applyRecipeFields(Recipe r, Map<String, Object> req) {
        if (req.containsKey("name")) r.setName((String) req.get("name"));
        if (req.containsKey("description")) r.setDescription((String) req.get("description"));
        if (req.containsKey("instruction")) r.setInstruction((String) req.get("instruction"));
        if (req.containsKey("prepTime"))
            r.setPrepTime(req.get("prepTime") != null ? ((Number) req.get("prepTime")).intValue() : null);
        if (req.containsKey("cookTime"))
            r.setCookTime(req.get("cookTime") != null ? ((Number) req.get("cookTime")).intValue() : null);
        if (req.containsKey("servings"))
            r.setServings(req.get("servings") != null ? ((Number) req.get("servings")).intValue() : null);
        if (req.containsKey("totalCalories"))
            r.setTotalCalories(req.get("totalCalories") != null ? ((Number) req.get("totalCalories")).intValue() : null);
        if (req.containsKey("requiredEquipment")) r.setRequiredEquipment((String) req.get("requiredEquipment"));
        if (req.containsKey("mainImageUrl")) r.setMainImageUrl((String) req.get("mainImageUrl"));
        if (req.containsKey("sourceUrl")) r.setSourceUrl((String) req.get("sourceUrl"));
        if (req.containsKey("favorite")) r.setFavorite((Boolean) req.get("favorite"));
        if (req.containsKey("tags") && req.get("tags") != null) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) req.get("tags");
            r.setTags(tags);
        }
    }

    @GetMapping("/units")
    public ResponseEntity<List<Map<String, String>>> units() {
        List<Map<String, String>> result = Arrays.stream(CulinaryUnit.values())
                .map(u -> Map.of("value", u.name(), "label", u.getDisplayName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/shopping-carts")
    public ResponseEntity<List<CartDto>> listCarts() {
        List<CartDto> carts = new ArrayList<>(shoppingCartService.load(new SearchRequest(), Pageable.ofSize(1000)))
                .stream()
                .filter(c -> !Boolean.TRUE.equals(c.isDeleted()))
                .sorted(Comparator.comparing(ShoppingCart::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toCartDto).collect(Collectors.toList());
        return ResponseEntity.ok(carts);
    }

    @GetMapping("/shopping-carts/{id}")
    public ResponseEntity<CartDto> getCart(@PathVariable UUID id) {
        return shoppingCartService.loadById(id)
                .map(c -> ResponseEntity.ok(toCartDto(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/shopping-carts")
    public ResponseEntity<CartDto> createCart(@RequestBody Map<String, Object> req) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(UUID.randomUUID());
        cart.setName((String) req.get("name"));
        cart.setArchived(false);
        cart.setModificationDate(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(toCartDto(shoppingCartService.save(cart)));
    }

    @DeleteMapping("/shopping-carts/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable UUID id) {
        return shoppingCartService.loadById(id).map(c -> {
            shoppingCartService.delete(c);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/shopping-carts/{id}/archive")
    public ResponseEntity<Void> archiveCart(@PathVariable UUID id) {
        return shoppingCartService.loadById(id).map(c -> {
            c.setArchived(true);
            shoppingCartService.save(c);
            return ResponseEntity.ok().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Shopping Carts ──────────────────────────────────────────────────────

    @PostMapping("/shopping-carts/{id}/add-recipe")
    public ResponseEntity<Void> addRecipeToCart(@PathVariable UUID id,
                                                @RequestParam UUID recipeId,
                                                @RequestParam(defaultValue = "1.0") double multiplier) {
        return recipeService.loadById(recipeId).map(recipe -> {
            shoppingCartService.addFromRecipe(id, recipe, multiplier);
            return ResponseEntity.ok().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/shopping-carts/{id}/items/{itemId}/toggle")
    public ResponseEntity<Void> toggleItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        return shoppingCartService.loadById(id).map(cart -> {
            shoppingCartService.togglePurchased(cart, itemId);
            return ResponseEntity.ok().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/shopping-carts/{id}/export")
    public ResponseEntity<String> exportCart(@PathVariable UUID id) {
        return ResponseEntity.ok(shoppingCartService.exportToText(id));
    }

    @PostMapping("/search")
    public ResponseEntity<List<RecipeMatchDto>> search(@RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<String> ingredients = (List<String>) req.getOrDefault("ingredients", Collections.emptyList());
        int minCoverage = req.containsKey("minCoverage")
                ? ((Number) req.get("minCoverage")).intValue() : 50;

        List<RecipeMatchDto> results = matchingEngine.findMatchingRecipes(ingredients, minCoverage)
                .stream().map(r -> new RecipeMatchDto(
                        r.getRecipe().getId(), r.getRecipe().getName(),
                        r.getRecipe().getAverageRating(), r.getRecipe().getRatingCount(),
                        r.getRecipe().getMainImageUrl(),
                        r.getMatchCount(), r.getCoveragePercent(),
                        r.getMatchedIngredients().stream().map(Ingredient::getName).collect(Collectors.toList()),
                        r.getMissingIngredients().stream().map(Ingredient::getName).collect(Collectors.toList())
                )).collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    record RecipeIngredientDto(UUID id, UUID ingredientId, String ingredientName,
                               String ingredientIcon, Double quantity, String unit,
                               String unitDisplayName, Boolean optional, String category,
                               String originalText) {
    }

    record RecipeDto(UUID id, String name, String description, String instruction,
                     Integer prepTime, Integer cookTime, Integer totalTime,
                     Integer servings, Integer totalCalories, Double averageRating,
                     Integer ratingCount, Boolean favorite, List<String> tags,
                     String requiredEquipment, String mainImageUrl, String sourceUrl,
                     List<RecipeIngredientDto> ingredients) {
    }

    record CartItemDto(UUID id, UUID ingredientId, String ingredientName,
                       Double quantity, String unit, String unitDisplayName,
                       Boolean purchased, UUID sourceRecipeId, String sourceRecipeName) {
    }

    record CartDto(UUID id, String name, Boolean archived, List<CartItemDto> items) {
    }

    // ─── Fridge Search ───────────────────────────────────────────────────────

    record RecipeMatchDto(UUID id, String name, Double averageRating, Integer ratingCount,
                          String mainImageUrl, int matchCount, double coveragePercent,
                          List<String> matched, List<String> missing) {
    }
}
