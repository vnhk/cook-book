package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.HistoryRecipe;
import com.bervan.history.model.BaseRepository;

import java.util.UUID;

public interface RecipeHistoryRepository extends BaseRepository<HistoryRecipe, UUID> {
}
