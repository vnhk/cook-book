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

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "Nabiał", "Mięso", "Ryby", "Warzywa", "Owoce", "Przyprawy",
            "Zboża", "Pieczywo", "Tłuszcze", "Napoje", "Słodycze",
            "Słone Przekąski", "Suplementy", "Inne"
    );

    public Ingredient findOrCreateByName(String name) {
        return findOrCreateByName(name, null);
    }

    public Ingredient findOrCreateByName(String name, String category) {
        String trimmed = name.trim();
        Optional<Ingredient> existing = ingredientRepository.findByNameIgnoreCase(trimmed);
        if (existing.isPresent() && !Boolean.TRUE.equals(existing.get().isDeleted())) {
            return existing.get();
        }

        Optional<IngredientAlias> alias = aliasRepository.findByAliasNameIgnoreCase(trimmed);
        if (alias.isPresent() && !Boolean.TRUE.equals(alias.get().isDeleted())
                && alias.get().getIngredient() != null
                && !Boolean.TRUE.equals(alias.get().getIngredient().isDeleted())) {
            return alias.get().getIngredient();
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setId(UUID.randomUUID());
        ingredient.setName(capitalizeFirst(trimmed));
        ingredient.setCategory(category != null && VALID_CATEGORIES.contains(category.trim())
                ? category.trim() : "Inne");
        return save(ingredient);
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
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
