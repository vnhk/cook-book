package com.bervan.cookbook.view;

import com.bervan.common.component.CommonComponentHelper;
import com.bervan.cookbook.model.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecipeComponentHelper extends CommonComponentHelper<UUID, Recipe> {
    private final List<String> allAvailableTags;

    public RecipeComponentHelper(List<String> allAvailableTags) {
        super(Recipe.class);
        this.allAvailableTags = allAvailableTags;
    }

    @Override
    protected List<String> getAllValuesForDynamicMultiDropdowns(String key, Recipe item) {
        if ("tags".equals(key)) {
            return allAvailableTags.stream().sorted(String::compareTo).toList();
        }
        return new ArrayList<>();
    }

    @Override
    protected List<String> getInitialSelectedValueForDynamicMultiDropdown(String key, Recipe item) {
        if ("tags".equals(key) && item != null) {
            List<String> tags = item.getTags();
            if (tags == null) {
                return new ArrayList<>();
            }
            return tags;
        }
        return new ArrayList<>();
    }
}
