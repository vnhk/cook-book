package com.bervan.cookbook.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedRecipeData {
    private String name;
    private String description;
    private String instructionHtml;
    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
    private Integer totalCalories;
    private Double averageRating;
    private Integer ratingCount;
    private String tags;
    private String requiredEquipment;
    private String sourceUrl;
    private String mainImageUrl;
    private List<ScrapedIngredientLine> ingredientLines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrapedIngredientLine {
        private String originalText;
        private Double quantity;
        private String unitText;
        private String ingredientText;
        private String category;
    }
}
