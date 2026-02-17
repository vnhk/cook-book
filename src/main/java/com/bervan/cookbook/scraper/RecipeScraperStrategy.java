package com.bervan.cookbook.scraper;

import com.bervan.cookbook.model.ScrapedRecipeData;

public interface RecipeScraperStrategy {
    String getName();

    ScrapedRecipeData scrape(String html);
}
