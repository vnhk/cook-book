package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.IngredientAlias;
import com.bervan.history.model.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientAliasRepository extends BaseRepository<IngredientAlias, UUID> {
    Optional<IngredientAlias> findByAliasNameIgnoreCase(String aliasName);

    List<IngredientAlias> findByAliasNameContainingIgnoreCase(String text);
}
