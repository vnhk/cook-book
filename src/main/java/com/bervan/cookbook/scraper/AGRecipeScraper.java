package com.bervan.cookbook.scraper;

import com.bervan.cookbook.model.ScrapedRecipeData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AGRecipeScraper implements RecipeScraperStrategy {
    private static final Logger log = LoggerFactory.getLogger(AGRecipeScraper.class);

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "^\\s*(\\d+[.,]?\\d*(?:\\s*[-–/]\\s*\\d+[.,]?\\d*)?)\\s*"
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
            .addTags("img")
            .addAttributes("img", "src", "srcset", "alt", "title", "width", "height", "loading", "sizes", "class")
            .addAttributes("a", "href", "title", "target")
            .addAttributes("div", "class", "style")
            .addAttributes("p", "class")
            .addAttributes("span", "class")
            .addAttributes("h2", "class")
            .addAttributes("h3", "class")
            .removeTags("button", "form", "input", "select", "textarea", "script", "style",
                    "nav", "header", "footer", "iframe", "ins");

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
        String instructionHtml = extractInstructionHtml(recipeElement);
        Integer prepTime = extractDurationMinutes(recipeElement, "prepTime");
        Integer cookTime = extractDurationMinutes(recipeElement, "cookTime");
        Integer servings = extractServings(recipeElement);
        Integer totalCalories = extractCalories(recipeElement);
        String mainImageUrl = extractMainImage(recipeElement);
        String tags = extractTags(recipeElement);
        Double averageRating = extractRatingValue(recipeElement);
        Integer ratingCount = extractRatingCount(recipeElement);

        List<ScrapedRecipeData.ScrapedIngredientLine> ingredientLines = extractIngredients(recipeElement);

        return ScrapedRecipeData.builder()
                .name(name)
                .description(description)
                .instructionHtml(instructionHtml)
                .prepTime(prepTime)
                .cookTime(cookTime)
                .servings(servings)
                .totalCalories(totalCalories)
                .averageRating(averageRating)
                .ratingCount(ratingCount)
                .tags(tags)
                .sourceUrl("")
                .mainImageUrl(mainImageUrl)
                .ingredientLines(ingredientLines)
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
        if (text == null) return null;
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
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
            String text = ing.text().trim();
            if (text.isEmpty()) continue;

            ScrapedRecipeData.ScrapedIngredientLine line = parseIngredientLine(text);
            lines.add(line);
        }

        return lines;
    }

    ScrapedRecipeData.ScrapedIngredientLine parseIngredientLine(String text) {
        String remaining = text.trim();
        Double quantity = null;
        String unitText = null;
        String ingredientText;

        Matcher qm = QUANTITY_PATTERN.matcher(remaining);
        if (qm.find()) {
            String qStr = qm.group(1).replace(",", ".").replaceAll("\\s+", "");
            if (qStr.contains("-") || qStr.contains("–")) {
                qStr = qStr.split("[-–]")[0].trim();
            }
            if (qStr.contains("/")) {
                String[] parts = qStr.split("/");
                try {
                    quantity = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    quantity = null;
                }
            } else {
                try {
                    quantity = Double.parseDouble(qStr);
                } catch (NumberFormatException e) {
                    quantity = null;
                }
            }
            if (quantity != null) {
                remaining = remaining.substring(qm.end()).trim();
            }
        }

        if (quantity != null) {
            Matcher um = UNIT_PATTERN.matcher(remaining);
            if (um.find()) {
                unitText = um.group(1).trim();
                remaining = remaining.substring(um.end()).trim();
            }
        }

        ingredientText = remaining;

        return new ScrapedRecipeData.ScrapedIngredientLine(
                text,
                quantity,
                unitText,
                ingredientText,
                null
        );
    }
}
