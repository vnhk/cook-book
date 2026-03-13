# Cook-Book App - Project Notes

> **IMPORTANT**: Keep this file updated when making significant changes to the codebase. This file serves as persistent memory between Claude Code sessions.

## Overview
Digital recipe management system. Store/organize recipes, manage ingredients with aliases, search recipes by available fridge ingredients, manage shopping carts, rate recipes, and import via web scraping. Polish localization throughout.

## Key Architecture

### Entities

#### Recipe
- `id: UUID`, `name: String` (2-300), `description/instruction: MEDIUMTEXT`
- `prepTime`, `cookTime`, `totalTime: Integer` (calculated), `servings`, `totalCalories: Integer`
- `averageRating: Double`, `ratingCount: Integer`, `tags: List<String>` (element collection)
- `favorite: Boolean`, `requiredEquipment: String`, `mainImageUrl: String` (max 1000)
- `sourceUrl: String` (max 300), `deleted`, `modificationDate`
- `recipeIngredients: OneToMany`, `history: OneToMany<HistoryRecipe>`

#### Ingredient
- `id: UUID`, `name: String` (1-200), `originalName: String`, `icon: String`, `category: String`
- Categories: Nabiał, Mięso, Ryby, Warzywa, Owoce, Przyprawy, Zboża, Tłuszcze, Napoje, Słodycze, Inne
- `aliases: OneToMany<IngredientAlias>`

#### RecipeIngredient (Junction)
- `recipe: ManyToOne`, `ingredient: ManyToOne`
- `quantity: Double`, `unit: CulinaryUnit`, `optional: Boolean`, `originalText: String` (max 300)

#### CulinaryUnit (Enum)
- Weight: GRAM, KILOGRAM, DEKAGRAM
- Volume: MILLILITER, LITER, TEASPOON, TABLESPOON, GLASS
- Count: PIECE, PINCH, BUNCH, CLOVE, SLICE, HANDFUL, PACK
- Each has: Polish display name, base type, conversion rate

#### ShoppingCart / ShoppingCartItem
- Cart: `id`, `name`, `archived`, `deleted`, `items: OneToMany`
- Item: `ingredient: ManyToOne`, `quantity`, `unit: CulinaryUnit`, `purchased: Boolean`, `sourceRecipe: ManyToOne`

#### RecipeRating
- `recipe: ManyToOne`, `rating: Integer` (1-5), `comment: String` (max 500)

### Services

#### RecipeService
- `save(Recipe)` — calculates `totalTime = prepTime + cookTime`
- `addRating(UUID, int, String)` — adds rating, recalculates average
- `toggleFavorite(UUID)`, `loadAllTags()`

#### IngredientService
- `findOrCreateByName(String)` — idempotent ingredient creation
- `addAlias(UUID, String)`, `searchByText(String, offset, limit)` — searches names + aliases

#### ShoppingCartService
- `addFromRecipe(UUID cartId, Recipe, double servingMultiplier)` — smart aggregation + unit conversion
- `togglePurchased(cart, itemId)`, `exportToText(cartId)` — plain text checklist

#### RecipeImportService
- Pluggable scraper strategies; `scrapePreview(scraperName, html)`, `importFromScraped(data)`
- HTML sanitization (allows images + basic formatting)

#### UnitConversionEngine
- `parseUnit(String)` — maps Polish/English text to CulinaryUnit (handles "łyżeczka", "g", "gram", etc.)
- `convert(quantity, from, to)`, `areCompatible(a, b)`, `formatQuantity(quantity, unit)`

#### IngredientNormalizationEngine
- Fuzzy ingredient matching with Jaccard similarity (score >= 0.4)
- `normalize(String)` — exact name → exact alias → fuzzy match
- `tokenize(String)` — strips Polish diacritics, filters noise words (duży, świeży, etc.)
- Polish noise words filtered: articles, prepositions, adjectives

#### RecipeMatchingEngine
- `findMatchingRecipes(List<String> fridge, double minCoverage)` — sorts by matchCount → coverage → rating
- Optional ingredients excluded from matching; returns matched/missing ingredient lists

### Views (Route prefix: `/cook-book/`)

| Route | View | Purpose |
|-------|------|---------|
| `recipes` | AbstractRecipeListView | Recipe grid with image, ratings, tags |
| `recipe-details/{id}` | AbstractRecipeDetailView | Full detail + inline edit + rating + cart |
| `ingredients` | AbstractIngredientListView | Ingredient management with categories |
| `shopping-cart` | AbstractShoppingCartView | Cart management + text export |
| `search` | AbstractRecipeSearchView | Fridge-based recipe discovery |

#### AbstractRecipeDetailView
- Add rating dialog, manage ingredients, add to cart with serving multiplier

#### AbstractRecipeSearchView
- Ingredient chips (Enter to add, click to remove), coverage threshold slider
- Results show: match count, coverage %, matched/missing ingredients

### UI Helpers
- `CookBookUIHelper` — star ratings, category badges, time badges, favorite icons, ingredient chips
- `RecipeComponentHelper` — recipe-specific UI components

## Configuration
- `src/main/resources/autoconfig/Recipe.yml` — all recipe fields; tags use `VaadinDynamicMultiDropdownBervanColumn`
- `src/main/resources/autoconfig/Ingredient.yml` — name + category (with predefined values)

## Important Notes
1. Polish localization: units in Polish, diacritical stripping (ą→a, ł→l, etc.)
2. Smart shopping cart: aggregates duplicate ingredients and converts units
3. Fuzzy ingredient matching handles Polish spelling variations
4. Web scraping: pluggable strategy pattern (AG scraper included)
5. Recipe instructions sanitized to allow safe HTML + images
6. Soft deletes on all entities; audit trail on Recipe via HistoryRecipe
7. Multi-tenancy via `BervanOwnedBaseEntity`
