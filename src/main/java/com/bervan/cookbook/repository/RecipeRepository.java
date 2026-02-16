package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.Recipe;
import com.bervan.history.model.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends BaseRepository<Recipe, UUID> {
    List<Recipe> findByNameContainingIgnoreCase(String name);
}
