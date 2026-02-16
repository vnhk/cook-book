package com.bervan.cookbook.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.cookbook.model.Recipe;
import com.bervan.cookbook.model.RecipeRating;
import com.bervan.cookbook.repository.RecipeHistoryRepository;
import com.bervan.cookbook.repository.RecipeRatingRepository;
import com.bervan.cookbook.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecipeService extends BaseService<UUID, Recipe> {
    private final RecipeRepository recipeRepository;
    private final RecipeHistoryRepository historyRepository;
    private final RecipeRatingRepository ratingRepository;

    public RecipeService(RecipeRepository repository, SearchService searchService,
                         RecipeHistoryRepository historyRepository,
                         RecipeRatingRepository ratingRepository) {
        super(repository, searchService);
        this.recipeRepository = repository;
        this.historyRepository = historyRepository;
        this.ratingRepository = ratingRepository;
    }

    public List<String> loadAllTags() {
        return recipeRepository.findAllDistinctTags();
    }

    @Override
    public Recipe save(Recipe recipe) {
        int prep = recipe.getPrepTime() != null ? recipe.getPrepTime() : 0;
        int cook = recipe.getCookTime() != null ? recipe.getCookTime() : 0;
        if (prep > 0 || cook > 0) {
            recipe.setTotalTime(prep + cook);
        }
        return super.save(recipe);
    }

    public void addRating(UUID recipeId, int rating, String comment) {
        Optional<Recipe> recipeOpt = loadById(recipeId);
        if (recipeOpt.isEmpty()) {
            return;
        }

        Recipe recipe = recipeOpt.get();

        RecipeRating recipeRating = new RecipeRating();
        recipeRating.setId(UUID.randomUUID());
        recipeRating.setRecipe(recipe);
        recipeRating.setRating(Math.max(1, Math.min(5, rating)));
        recipeRating.setComment(comment);
        recipeRating.getOwners().addAll(recipe.getOwners());
        ratingRepository.save(recipeRating);

        recalculateRating(recipe);
    }

    public void toggleFavorite(UUID recipeId) {
        Optional<Recipe> recipeOpt = loadById(recipeId);
        if (recipeOpt.isPresent()) {
            Recipe recipe = recipeOpt.get();
            recipe.setFavorite(!Boolean.TRUE.equals(recipe.getFavorite()));
            save(recipe);
        }
    }

    private void recalculateRating(Recipe recipe) {
        List<RecipeRating> ratings = ratingRepository.findByRecipeId(recipe.getId());
        List<RecipeRating> activeRatings = ratings.stream()
                .filter(r -> !Boolean.TRUE.equals(r.isDeleted()))
                .toList();

        if (activeRatings.isEmpty()) {
            recipe.setAverageRating(null);
            recipe.setRatingCount(0);
        } else {
            double avg = activeRatings.stream()
                    .mapToInt(RecipeRating::getRating)
                    .average()
                    .orElse(0);
            recipe.setAverageRating(Math.round(avg * 10.0) / 10.0);
            recipe.setRatingCount(activeRatings.size());
        }
        save(recipe);
    }
}
