package com.bervan.cookbook.scraper;

import com.bervan.cookbook.model.ScrapedRecipeData;

public interface RecipeScraperStrategy {
    boolean supports(String url);

    ScrapedRecipeData scrape(String url);
}
