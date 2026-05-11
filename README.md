# cook-book

Digital recipe manager. Store and organize recipes, manage ingredients with aliases, discover recipes by available fridge contents, manage shopping carts, rate recipes, and import via web scraping.

## Features

- **Fridge-based search**: Enter available ingredients, get matching recipes sorted by coverage and rating
- **Smart shopping cart**: Aggregates duplicate ingredients and converts units automatically
- **Ingredient aliases**: Fuzzy matching handles spelling variations and Polish diacritics
- **Recipe import**: Pluggable web scraper strategies
- **Diet dashboard**: Charts for calorie intake, activity, net deficit, and weight over time
- **Multi-tenancy**: Each user sees only their own data

## Key Entities

| Entity | Description |
|--------|-------------|
| `Recipe` | Full recipe with prep/cook time, servings, calories, tags, ratings |
| `Ingredient` | Ingredient with category, icon, and aliases |
| `RecipeIngredient` | Junction: recipe + ingredient + quantity + unit |
| `ShoppingCart` / `ShoppingCartItem` | Cart with purchased tracking |
| `RecipeRating` | 1–5 star rating with optional comment |

## Routes (`/cook-book/`)

| Path | Purpose |
|------|---------|
| `recipes` | Recipe grid with images and ratings |
| `recipe-details/{id}` | Full detail, inline edit, rating, add to cart |
| `ingredients` | Ingredient management by category |
| `shopping-cart` | Cart management + text export |
| `search` | Fridge-based recipe discovery |
| `diet-dashboard` | Calorie and weight tracking charts |

## Key Services

- `RecipeMatchingEngine` — fuzzy fridge-to-recipe matching (Jaccard similarity ≥ 0.4)
- `IngredientNormalizationEngine` — strips Polish diacritics, filters noise words
- `UnitConversionEngine` — converts Polish/English culinary units
- `ShoppingCartService` — smart aggregation with unit conversion
- `RecipeImportService` — pluggable HTML scraper strategies

## Supported Units

Weight (g, kg, dag), Volume (ml, l, teaspoon, tablespoon, glass), Count (piece, pinch, bunch, clove, slice, handful, pack)

## Build

```bash
mvn clean install -DskipTests
```

Part of the `my-tools` multi-module Maven project. Requires `common` to be built first.
