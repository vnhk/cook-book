package com.bervan.cookbook.scraper;

import com.bervan.cookbook.model.ScrapedRecipeData;
import com.bervan.cookbook.service.RecipeAIScraperService;
import com.bervan.logging.JsonLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AGRecipeScraper implements RecipeScraperStrategy {
    private static final JsonLogger log = JsonLogger.getLogger(AGRecipeScraper.class, "cook-book");

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "^\\s*(\\d+[.,]?\\d*(?:\\s*[-–/]\\s*\\d+[.,]?\\d*)?)\\s*"
    );

    private static final Pattern TRAILING_METRIC_PATTERN = Pattern.compile(
            "(?:ok\\.?\\s*|około\\s*)?(\\d+[.,]?\\d*)\\s*(g|kg|dag|dkg|ml|l)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "^(g|kg|dag|dkg|ml|l|szt\\.?|sztuk[aiy]?|łyżeczk[aiy]?|łyżk[aiy]?|" +
                    "łyżeczek|łyżek|szklan(?:ka|ki|ek)|szczyp(?:ta|ty|t)|" +
                    "pęcz(?:ek|ki|ków)|ząb(?:ek|ki|ków)|plaster(?:ek|ki|ków)|" +
                    "garś(?:ć|cie|ci)|opakowa(?:nie|nia|ń)|" +
                    "lyzeczk[aiy]?|lyzk[aiy]?|peczk[aiy]?|zabk[aiy]?|garsc[ie]?)\\b\\s*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Safelist INSTRUCTION_SAFELIST = Safelist.relaxed()
            .addTags("img", "h4", "section", "article")
            .addAttributes("img", "src", "srcset", "alt", "title", "width", "height", "loading", "sizes", "class")
            .addAttributes("a", "href", "title", "target")
            .addAttributes("div", "class", "style")
            .addAttributes("p", "class")
            .addAttributes("span", "class")
            .addAttributes("h2", "class")
            .addAttributes("h3", "class")
            .addAttributes("h4", "class")
            .addAttributes("ol", "class")
            .addAttributes("ul", "class")
            .addAttributes("li", "class")
            .removeTags("button", "form", "input", "select", "textarea", "script", "style",
                    "nav", "header", "footer", "iframe", "ins");

    private final RecipeAIScraperService aiScraperService;

    public AGRecipeScraper() {
        this.aiScraperService = null;
    }

    @Autowired
    public AGRecipeScraper(RecipeAIScraperService aiScraperService) {
        this.aiScraperService = aiScraperService;
    }

    @Override
    public String getName() {
        return "AG";
    }

    @Override
    public ScrapedRecipeData scrape(String html) {
        log.info("Parsing recipe HTML with AGRecipeScraper, length: {}", html.length());
        Document doc = Jsoup.parse(html);
        return parseRecipe(doc);
    }

    private ScrapedRecipeData parseRecipe(Document doc) {
        Element recipeElement = doc.selectFirst("[itemtype*='schema.org/Recipe']");
        if (recipeElement == null) {
            recipeElement = doc;
        }

        String name = extractName(recipeElement);
        String description = extractDescription(recipeElement);
        String rawInstructionHtml = extractInstructionHtml(recipeElement);
        Integer prepTime = extractDurationMinutes(recipeElement, "prepTime");
        Integer cookTime = extractDurationMinutes(recipeElement, "cookTime");
        Integer servings = extractServings(recipeElement);
        Integer totalCalories = extractCalories(recipeElement);
        String mainImageUrl = extractMainImage(recipeElement);
        String tags = extractTags(recipeElement);
        Double averageRating = extractRatingValue(recipeElement);
        Integer ratingCount = extractRatingCount(recipeElement);

        List<ScrapedRecipeData.ScrapedIngredientLine> ingredientLines = extractIngredients(recipeElement);
        String fullRecipeContext = buildFullRecipeContext(recipeElement, name, description, ingredientLines);

        List<ScrapedRecipeData.ScrapedIngredientLine> finalIngredients = ingredientLines;
        String finalInstructionHtml = rawInstructionHtml;

        if (aiScraperService != null) {
            Element ingContainer = recipeElement.selectFirst("#recipeIngredients");
            String rawIngredientsHtml = ingContainer != null
                    ? ingContainer.html()
                    : recipeElement.select("[itemprop=recipeIngredient], .recipe-ing-list li").outerHtml();

            finalIngredients = aiScraperService.unifyIngredients(rawIngredientsHtml, ingredientLines);
            finalInstructionHtml = aiScraperService.summarizeInstructions(
                    rawInstructionHtml, fullRecipeContext, finalIngredients,
                    recipeElement.baseUri(), INSTRUCTION_SAFELIST);
        }

        return ScrapedRecipeData.builder()
                .name(name)
                .description(description)
                .instructionHtml(finalInstructionHtml)
                .prepTime(prepTime)
                .cookTime(cookTime)
                .servings(servings)
                .totalCalories(totalCalories)
                .averageRating(averageRating)
                .ratingCount(ratingCount)
                .tags(tags)
                .sourceUrl("")
                .mainImageUrl(mainImageUrl)
                .ingredientLines(finalIngredients)
                .build();
    }

    private String extractName(Element root) {
        Element el = root.selectFirst("h1[itemprop=name]");
        if (el != null) return el.text().trim();

        el = root.selectFirst("[itemprop=name]");
        if (el != null && "h1".equalsIgnoreCase(el.tagName())) {
            return el.text().trim();
        }

        el = root.selectFirst("[itemprop=name]");
        if (el != null) return el.text().trim();

        el = root.selectFirst("h1");
        if (el != null) return el.text().trim();

        return "Imported Recipe";
    }

    private String extractDescription(Element root) {
        Element el = root.selectFirst("meta[itemprop=description]");
        if (el != null) return el.attr("content").trim();

        el = root.selectFirst("[itemprop=description]");
        if (el != null) return el.text().trim();

        el = root.selectFirst(".article-intro");
        if (el != null) return el.text().trim();

        return null;
    }

    private String extractInstructionHtml(Element root) {
        Element instructionEl = root.selectFirst("[itemprop=recipeInstructions]");
        if (instructionEl == null) {
            instructionEl = root.selectFirst(".article-content-body");
        }
        if (instructionEl == null) {
            return null;
        }

        Element copy = instructionEl.clone();

        copy.select("script, style, button, form, input, select, textarea, nav, " +
                "header, footer, iframe, ins, .adsbygoogle, .ads-slot-article, " +
                ".copy-share-lock-con, .share-ingredients, .wake-photo-con, " +
                ".article-intro, #recipeIngredients, .recipe-info, .nutrition-info, " +
                ".share-recipe, .print-it, .article-nav, .ing-header, .ing-remarks, " +
                "[data-nosnippet], .ad-slot, .comm-ad-slot").remove();

        String html = copy.html();

        return Jsoup.clean(html, root.baseUri(), INSTRUCTION_SAFELIST);
    }

    private String buildFullRecipeContext(Element root,
                                          String name,
                                          String description,
                                          List<ScrapedRecipeData.ScrapedIngredientLine> rawIngredients) {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append("Nazwa dania: ").append(name).append("\n");
        }
        if (description != null && !description.isBlank()) {
            sb.append("Opis: ").append(description).append("\n");
        }
        Element recipeInfo = root.selectFirst(".recipe-info");
        if (recipeInfo != null) {
            sb.append("Informacje o przepisie: ").append(recipeInfo.text().trim()).append("\n");
        }
        Element ingContainer = root.selectFirst("#recipeIngredients");
        if (ingContainer != null) {
            sb.append("\nPełna sekcja składników z przepisu:\n").append(ingContainer.text().trim()).append("\n");
        } else if (rawIngredients != null && !rawIngredients.isEmpty()) {
            sb.append("\nLista składników:\n");
            for (ScrapedRecipeData.ScrapedIngredientLine line : rawIngredients) {
                sb.append("- ").append(line.getOriginalText()).append("\n");
            }
        }
        Element instructionEl = root.selectFirst("[itemprop=recipeInstructions]");
        if (instructionEl == null) {
            instructionEl = root.selectFirst(".article-content-body");
        }
        if (instructionEl != null) {
            sb.append("\nPełna treść przygotowania przepisu:\n").append(instructionEl.text().trim()).append("\n");
        }
        return sb.toString();
    }

    private Integer extractDurationMinutes(Element root, String itemprop) {
        Element meta = root.selectFirst("meta[itemprop=" + itemprop + "]");
        if (meta != null) {
            return parseIsoDuration(meta.attr("content"));
        }

        Element el = root.selectFirst("[itemprop=" + itemprop + "]");
        if (el != null) {
            String content = el.attr("content");
            if (!content.isEmpty()) return parseIsoDuration(content);
            String datetime = el.attr("datetime");
            if (!datetime.isEmpty()) return parseIsoDuration(datetime);
            return parseMinutesFromText(el.text());
        }

        return null;
    }

    private Integer parseIsoDuration(String iso) {
        if (iso == null || iso.isBlank()) return null;
        Pattern p = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?");
        Matcher m = p.matcher(iso.toUpperCase());
        if (m.find()) {
            int hours = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int minutes = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            return hours * 60 + minutes;
        }
        return null;
    }

    private Integer parseMinutesFromText(String text) {
        return parseFirstNumber(text);
    }

    private Integer extractServings(Element root) {
        Element meta = root.selectFirst("meta[itemprop=recipeYield]");
        if (meta != null) {
            return parseFirstNumber(meta.attr("content"));
        }
        Element el = root.selectFirst("[itemprop=recipeYield]");
        if (el != null) return parseFirstNumber(el.text());
        return null;
    }

    private Integer extractCalories(Element root) {
        Element el = root.selectFirst("[itemprop=calories]");
        if (el != null) return parseFirstNumber(el.text());
        return null;
    }

    private Integer parseFirstNumber(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
    }

    private Double extractRatingValue(Element root) {
        Element el = root.selectFirst("[itemprop=ratingValue]");
        if (el != null) {
            try {
                return Double.parseDouble(el.text().trim().replace(",", "."));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Integer extractRatingCount(Element root) {
        Element el = root.selectFirst("[itemprop=ratingCount]");
        if (el != null) return parseFirstNumber(el.text());
        return null;
    }

    private String extractMainImage(Element root) {
        Element meta = root.selectFirst("meta[itemprop=image]");
        if (meta != null) {
            String url = meta.attr("content");
            if (!url.isEmpty()) return url;
        }

        Element img = root.selectFirst("[itemprop=image]");
        if (img != null) {
            if ("img".equalsIgnoreCase(img.tagName())) return img.attr("abs:src");
            return img.attr("content");
        }

        Element mainImg = root.selectFirst(".article-main-img img");
        if (mainImg != null) return mainImg.attr("abs:src");

        return null;
    }

    private String extractTags(Element root) {
        Set<String> tags = new LinkedHashSet<>();

        Element keywords = root.selectFirst("meta[itemprop=keywords]");
        if (keywords != null) {
            String content = keywords.attr("content");
            if (!content.isEmpty()) {
                for (String tag : content.split(",")) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) tags.add(trimmed);
                }
            }
        }

        Element category = root.selectFirst("meta[itemprop=recipeCategory]");
        if (category != null) {
            String cat = category.attr("content").trim();
            if (!cat.isEmpty()) tags.add(cat);
        }

        Elements diets = root.select("[itemprop=suitableForDiet]");
        for (Element diet : diets) {
            String href = diet.attr("href");
            if (href.contains("Vegetarian")) tags.add("wegetariańska");
            else if (href.contains("Vegan")) tags.add("wegańska");
            else if (href.contains("GlutenFree")) tags.add("bezglutenowa");
            else if (href.contains("DiabeticDiet")) tags.add("diabetyczna");
            else if (href.contains("LowCalorie")) tags.add("niskokaloryczna");
        }

        Elements tagLinks = root.select(".post-tags .category a");
        for (Element tagLink : tagLinks) {
            String text = tagLink.text().trim();
            if (!text.isEmpty()) tags.add(text);
        }

        Elements catLinks = root.select(".post-categories .category a");
        for (Element catLink : catLinks) {
            String text = catLink.text().trim();
            if (!text.isEmpty()) tags.add(text);
        }

        return tags.isEmpty() ? null : String.join(", ", tags);
    }

    private List<ScrapedRecipeData.ScrapedIngredientLine> extractIngredients(Element root) {
        List<ScrapedRecipeData.ScrapedIngredientLine> lines = new ArrayList<>();

        Elements ingredients = root.select("[itemprop=recipeIngredient]");
        if (ingredients.isEmpty()) {
            ingredients = root.select(".recipe-ing-list li");
        }

        for (Element ing : ingredients) {
            Element nameSpan = ing.selectFirst(".ingredient, .ing-name, .name");
            Element qtySpan = ing.selectFirst(".qty, .ing-num, .amount");

            String text;
            if (nameSpan != null && qtySpan != null) {
                text = nameSpan.text().trim() + " - " + qtySpan.text().trim();
            } else {
                text = ing.text().trim();
            }
            if (text.isEmpty()) continue;

            String sectionCategory = findSectionCategory(ing);
            ScrapedRecipeData.ScrapedIngredientLine line = parseIngredientLine(text);
            if (sectionCategory != null && line.getCategory() == null) {
                line.setCategory(sectionCategory);
            }
            lines.add(line);
        }

        return lines;
    }

    private String findSectionCategory(Element ingElement) {
        Element ul = ingElement.closest("ul, ol");
        if (ul != null) {
            Element prev = ul.previousElementSibling();
            if (prev != null && (prev.hasClass("ing-header") || prev.tagName().matches("h[2-5]"))) {
                return prev.text().trim();
            }
        }
        return null;
    }

    ScrapedRecipeData.ScrapedIngredientLine parseIngredientLine(String text) {
        String remaining = text.trim();
        Double quantity = null;
        String unitText = null;
        String ingredientText;
        boolean optional = remaining.toLowerCase().contains("opcjonalnie");

        // Case 1: "Nazwa składnika - 100 g" or "Nazwa składnika - 2 szklanki - 320 g" (AniaGotuje format)
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
                    quantity = pqu.quantity;
                    unitText = pqu.unitText;
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

        // Case 2: "100 g dyni ABC" or "100g dyni ABC" (Quantity at start)
        ParsedQtyUnit pqu = parseLeadingQuantityAndUnit(remaining);
        quantity = pqu.quantity;
        unitText = pqu.unitText;
        ingredientText = capitalizeFirst(pqu.remainingText);

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

    private record ParsedQtyUnit(Double quantity, String unitText, String remainingText) {
    }
}
