package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.Recipe;
import com.bervan.history.model.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends BaseRepository<Recipe, UUID> {
    List<Recipe> findByNameContainingIgnoreCase(String name);

    @Query("SELECT DISTINCT t FROM Recipe r JOIN r.tags t ORDER BY t")
    List<String> findAllDistinctTags();
}
