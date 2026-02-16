package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.RecipeRating;
import com.bervan.history.model.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRatingRepository extends BaseRepository<RecipeRating, UUID> {
    List<RecipeRating> findByRecipeId(UUID recipeId);
}
