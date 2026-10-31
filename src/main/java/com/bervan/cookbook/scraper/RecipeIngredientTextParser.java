package com.bervan.cookbook.scraper;

import com.bervan.cookbook.model.ScrapedRecipeData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RecipeIngredientTextParser {

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "^\\s*(\\d+[.,]?\\d*(?:\\s*[-–/]\\s*\\d+[.,]?\\d*)?)\\s*"
    );

    private static final Pattern TRAILING_METRIC_PATTERN = Pattern.compile(
            "(?:ok\\.?\\s*|około\\s*)?(\\d+[.,]?\\d*)\\s*(g|kg|dag|dkg|ml|l)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "^(g|kg|dag|dkg|ml|l|szt\\.?|" +
                    "sztuk[a-ząćęłńóśźż]*|" +
                    "łyżeczk[a-ząćęłńóśźż]*|łyżeczce|" +
                    "łyżk[a-ząćęłńóśźż]*|łyżce|" +
                    "szklank[a-ząćęłńóśźż]*|szklance|szklanek|" +
                    "szczypt[a-ząćęłńóśźż]*|szczypcie|" +
                    "pęcz(?:ek|ka|ku|kiem|ki|ków)|" +
                    "ząb(?:ek|ka|ku|kiem|ki|ków)|" +
                    "plaster(?:ek|ka|ku|kiem|ki|ków)?|" +
                    "garś(?:ć|ci|cią|cie)|" +
                    "opakowa(?:nie|nia|niu|niem|ń)|" +
                    "lyzeczk[a-z]*|lyzeczce|lyzk[a-z]*|lyzce|peczk[a-z]*|zabk[a-z]*|garsc[a-z]*)\\b\\s*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public ScrapedRecipeData.ScrapedIngredientLine parseIngredientLine(String text) {
        if (text == null || text.isBlank()) {
            return ScrapedRecipeData.ScrapedIngredientLine.builder().build();
        }
        String remaining = text.trim();
        Double quantity = null;
        String unitText = null;
        String ingredientText;
        boolean optional = remaining.toLowerCase().contains("opcjonalnie");

        // Case 1: "Nazwa składnika - 100 g" or "Nazwa składnika - 2 szklanki - 320 g"
        if (remaining.contains(" - ") || remaining.contains(" – ")) {
            String[] parts = remaining.split("\\s+[-–]\\s+", 2);
            String firstPart = parts[0].trim();
            String secondPart = parts[1].trim();

            if (!QUANTITY_PATTERN.matcher(firstPart).find()) {
                ingredientText = capitalizeFirst(firstPart);
                Matcher metricMatcher = TRAILING_METRIC_PATTERN.matcher(secondPart);
                if (metricMatcher.find()) {
                    quantity = parseQuantityValue(metricMatcher.group(1));
                    unitText = metricMatcher.group(2).trim();
                } else {
                    ParsedQtyUnit pqu = parseLeadingQuantityAndUnit(secondPart);
                    quantity = pqu.quantity();
                    unitText = pqu.unitText();
                }
                String formattedOriginal = formatOriginalText(ingredientText, quantity, unitText, text);
                return ScrapedRecipeData.ScrapedIngredientLine.builder()
                        .originalText(formattedOriginal)
                        .quantity(quantity)
                        .unitText(unitText)
                        .ingredientText(ingredientText)
                        .optional(optional)
                        .build();
            }
        }

        // Case 2: "100 g dyni ABC" (Quantity at start)
        ParsedQtyUnit pqu = parseLeadingQuantityAndUnit(remaining);
        quantity = pqu.quantity();
        unitText = pqu.unitText();
        ingredientText = capitalizeFirst(pqu.remainingText());

        String formattedOriginal = formatOriginalText(ingredientText, quantity, unitText, text);
        return ScrapedRecipeData.ScrapedIngredientLine.builder()
                .originalText(formattedOriginal)
                .quantity(quantity)
                .unitText(unitText)
                .ingredientText(ingredientText)
                .optional(optional)
                .build();
    }

    private ParsedQtyUnit parseLeadingQuantityAndUnit(String input) {
        String remaining = input.trim();
        Double quantity = null;
        String unitText = null;

        String lower = remaining.toLowerCase();
        if (lower.startsWith("półtorej ") || lower.startsWith("półtora ")) {
            quantity = 1.5;
            remaining = remaining.substring(remaining.indexOf(' ') + 1).trim();
        } else if (lower.startsWith("pół ")) {
            quantity = 0.5;
            remaining = remaining.substring(4).trim();
        } else if (lower.startsWith("ćwierć ")) {
            quantity = 0.25;
            remaining = remaining.substring(7).trim();
        } else {
            Matcher qm = QUANTITY_PATTERN.matcher(remaining);
            if (qm.find()) {
                quantity = parseQuantityValue(qm.group(1));
                if (quantity != null) {
                    remaining = remaining.substring(qm.end()).trim();
                }
            }
        }

        if (quantity != null) {
            Matcher um = UNIT_PATTERN.matcher(remaining);
            if (um.find()) {
                unitText = um.group(1).trim();
                remaining = remaining.substring(um.end()).trim();
            }
        }

        return new ParsedQtyUnit(quantity, unitText, remaining);
    }

    private Double parseQuantityValue(String raw) {
        if (raw == null) return null;
        String qStr = raw.replace(",", ".").replaceAll("\\s+", "");
        if (qStr.contains("-") || qStr.contains("–")) {
            qStr = qStr.split("[-–]")[0].trim();
        }
        if (qStr.contains("/")) {
            String[] parts = qStr.split("/");
            try {
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Double.parseDouble(qStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatOriginalText(String ingredientText, Double quantity, String unitText, String fallback) {
        if (ingredientText != null && !ingredientText.isBlank() && quantity != null) {
            String qtyStr = quantity == Math.floor(quantity)
                    ? String.valueOf(quantity.intValue())
                    : String.valueOf(quantity);
            String unitStr = (unitText != null && !unitText.isBlank()) ? " " + unitText : "";
            return ingredientText + " - " + qtyStr + unitStr;
        }
        return fallback;
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isBlank()) return text;
        String trimmed = text.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    public record ParsedQtyUnit(Double quantity, String unitText, String remainingText) {
    }
}
