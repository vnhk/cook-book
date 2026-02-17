package com.bervan.cookbook.view;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class CookBookUIHelper {

    public static HorizontalLayout createRatingStars(Double rating, Integer ratingCount) {
        HorizontalLayout stars = new HorizontalLayout();
        stars.addClassName("cb-rating-stars");
        stars.setSpacing(false);

        double r = rating != null ? rating : 0;
        for (int i = 1; i <= 5; i++) {
            Icon star;
            if (i <= r) {
                star = VaadinIcon.STAR.create();
                star.addClassName("cb-star-filled");
            } else if (i - 0.5 <= r) {
                star = VaadinIcon.STAR.create();
                star.addClassName("cb-star-half");
            } else {
                star = VaadinIcon.STAR_O.create();
                star.addClassName("cb-star-empty");
            }
            star.setSize("16px");
            stars.add(star);
        }

        if (rating != null) {
            Span ratingText = new Span(String.format("%.1f", rating));
            ratingText.getStyle().set("font-size", "var(--bervan-font-size-xs, 0.75rem)")
                    .set("color", "var(--bervan-text-secondary)")
                    .set("margin-left", "4px");
            stars.add(ratingText);
        }

        if (ratingCount != null && ratingCount > 0) {
            Span countText = new Span("(" + formatRatingCount(ratingCount) + ")");
            countText.getStyle().set("font-size", "var(--bervan-font-size-xs, 0.75rem)")
                    .set("color", "var(--bervan-text-tertiary, #64748b)")
                    .set("margin-left", "4px");
            stars.add(countText);
        }

        return stars;
    }

    public static String formatRatingCount(int count) {
        if (count >= 1000) return "+" + (count / 1000) + "k";
        if (count >= 500) return "+500";
        if (count >= 100) return "+100";
        if (count >= 50) return "+50";
        if (count >= 10) return "+10";
        return String.valueOf(count);
    }

    public static Span createCategoryBadge(String category) {
        Span badge = new Span(category != null ? category : "Inne");
        badge.addClassName("cb-category-badge");
        if (category != null) {
            badge.addClassName("cb-category-" + category.toLowerCase()
                    .replace("ą", "a").replace("ę", "e").replace("ó", "o")
                    .replace("ś", "s").replace("ł", "l").replace("ż", "z")
                    .replace("ź", "z").replace("ć", "c").replace("ń", "n"));
        }
        return badge;
    }

    public static Span createTimeBadge(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return new Span("-");
        }
        String text;
        if (minutes >= 60) {
            int h = minutes / 60;
            int m = minutes % 60;
            text = m > 0 ? h + "h " + m + "min" : h + "h";
        } else {
            text = minutes + "min";
        }
        Span badge = new Span(text);
        badge.addClassName("cb-time-badge");
        return badge;
    }

    public static Icon createFavoriteIcon(Boolean favorite) {
        Icon icon;
        if (Boolean.TRUE.equals(favorite)) {
            icon = VaadinIcon.HEART.create();
            icon.addClassName("cb-favorite-active");
        } else {
            icon = VaadinIcon.HEART_O.create();
        }
        icon.addClassName("cb-favorite-btn");
        icon.setSize("20px");
        return icon;
    }

    public static Span createIngredientChip(String name, boolean matched) {
        Span chip = new Span(name);
        chip.addClassName("cb-ingredient-chip");
        chip.addClassName(matched ? "cb-ingredient-matched" : "cb-ingredient-missing");
        return chip;
    }
}
