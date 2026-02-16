package com.bervan.cookbook.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.model.IngredientAlias;
import com.bervan.cookbook.repository.IngredientAliasRepository;
import com.bervan.cookbook.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IngredientService extends BaseService<UUID, Ingredient> {
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;

    public IngredientService(IngredientRepository repository, SearchService searchService,
                             IngredientAliasRepository aliasRepository) {
        super(repository, searchService);
        this.ingredientRepository = repository;
        this.aliasRepository = aliasRepository;
    }

    public Ingredient findOrCreateByName(String name) {
        Optional<Ingredient> existing = ingredientRepository.findByNameIgnoreCase(name.trim());
        if (existing.isPresent()) {
            return existing.get();
        }

        Optional<IngredientAlias> alias = aliasRepository.findByAliasNameIgnoreCase(name.trim());
        if (alias.isPresent()) {
            return alias.get().getIngredient();
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setId(UUID.randomUUID());
        ingredient.setName(name.trim());
        ingredient.setCategory("Inne");
        return save(ingredient);
    }

    public void addAlias(UUID ingredientId, String aliasName) {
        Optional<Ingredient> ingredient = loadById(ingredientId);
        if (ingredient.isPresent()) {
            IngredientAlias alias = new IngredientAlias();
            alias.setId(UUID.randomUUID());
            alias.setAliasName(aliasName.trim());
            alias.setIngredient(ingredient.get());
            ingredient.get().getAliases().add(alias);
            save(ingredient.get());
        }
    }

    public List<Ingredient> searchByText(String text, int offset, int limit) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String searchText = text.trim();

        List<Ingredient> byName = ingredientRepository.findByNameContainingIgnoreCase(searchText);
        List<IngredientAlias> byAlias = aliasRepository.findByAliasNameContainingIgnoreCase(searchText);

        Set<UUID> seen = new HashSet<>();
        List<Ingredient> results = new ArrayList<>();

        Stream.concat(byName.stream(), byAlias.stream().map(IngredientAlias::getIngredient))
                .filter(i -> i.isDeleted() == null || !i.isDeleted())
                .forEach(i -> {
                    if (seen.add(i.getId())) {
                        results.add(i);
                    }
                });

        int end = Math.min(offset + limit, results.size());
        if (offset >= results.size()) {
            return Collections.emptyList();
        }
        return results.subList(offset, end);
    }
}
