package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.Ingredient;
import com.bervan.history.model.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends BaseRepository<Ingredient, UUID> {
    Optional<Ingredient> findByNameIgnoreCase(String name);

    List<Ingredient> findByNameContainingIgnoreCase(String name);
}
