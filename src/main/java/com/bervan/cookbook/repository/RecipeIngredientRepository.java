package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.RecipeIngredient;
import com.bervan.history.model.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeIngredientRepository extends BaseRepository<RecipeIngredient, UUID> {
    List<RecipeIngredient> findByRecipeId(UUID recipeId);
}
