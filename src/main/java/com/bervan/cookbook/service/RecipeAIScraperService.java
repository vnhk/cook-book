package com.bervan.cookbook.service;

import com.bervan.common.service.OpenAIService;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.model.ScrapedRecipeData;
import com.bervan.cookbook.repository.IngredientRepository;
import com.bervan.logging.JsonLogger;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecipeAIScraperService {
    private static final JsonLogger log = JsonLogger.getLogger(RecipeAIScraperService.class, "cook-book");

    private static final Safelist PROMPT_HTML_SAFELIST = Safelist.basic()
            .addTags("h1", "h2", "h3", "h4", "section", "article", "div");

    private static final Pattern NUMBER_EXTRACTOR = Pattern.compile("(\\d+[.,]?\\d*)");

    private static final String AI_INSTRUCTION_SYSTEM_PROMPT =
            "Jesteś doświadczonym redaktorem kulinarnym specjalizującym się w tworzeniu krótkich, " +
            "przejrzystych i praktycznych instrukcji gotowania w formacie HTML.";

    private static final String AI_INGREDIENT_SYSTEM_PROMPT =
            "Jesteś ekspertem kulinarnym i analitykiem danych. Twoim zadaniem jest dokładna ekstrakcja i " +
            "ujednolicenie (unifikacja) listy składników z przepisu kulinarnego do czystego formatu JSON, " +
            "ze szczególnym uwzględnieniem bezbłędnego rozbijania zbiorczych list przypraw na pojedyncze składniki.";

    private final String apiKey;
    private final IngredientRepository ingredientRepository;
    private final OpenAIService instructionAIService;
    private final OpenAIService ingredientAIService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    public RecipeAIScraperService(@Value("${openai.api.key:}") String apiKey,
                                  @Autowired(required = false) IngredientRepository ingredientRepository) {
        this.apiKey = apiKey;
        this.ingredientRepository = ingredientRepository;
        this.instructionAIService = new OpenAIService(AI_INSTRUCTION_SYSTEM_PROMPT);
        this.ingredientAIService = new OpenAIService(AI_INGREDIENT_SYSTEM_PROMPT);
    }

    public String summarizeInstructions(String rawInstructionHtml,
                                        String fullRecipeContext,
                                        List<ScrapedRecipeData.ScrapedIngredientLine> unifiedIngredients,
                                        String baseUri,
                                        Safelist safelist) {
        if (rawInstructionHtml == null || rawInstructionHtml.isBlank()) {
            return rawInstructionHtml;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.info("OpenAI API key is not configured, returning raw cleaned instruction HTML.");
            return rawInstructionHtml;
        }

        StringBuilder ingredientsSummary = new StringBuilder();
        if (unifiedIngredients != null && !unifiedIngredients.isEmpty()) {
            for (int i = 0; i < unifiedIngredients.size(); i++) {
                ScrapedRecipeData.ScrapedIngredientLine ing = unifiedIngredients.get(i);
                String varName = (ing.getVariableName() != null && !ing.getVariableName().isBlank())
                        ? ing.getVariableName()
                        : "ingredient" + (i + 1);
                ing.setVariableName(varName);

                ingredientsSummary.append(String.format("- {%s}: %s (oryginalnie: %s)%s\n",
                        varName,
                        ing.getIngredientText() != null ? ing.getIngredientText() : ing.getOriginalText(),
                        ing.getOriginalText() != null ? ing.getOriginalText() : (ing.getQuantity() + " " + (ing.getUnitText() != null ? ing.getUnitText() : "")),
                        ing.getCategory() != null && !ing.getCategory().isBlank() ? " [" + ing.getCategory() + "]" : ""
                ));
            }
        }

        String promptInstructionHtml = prepareHtmlForAiPrompt(rawInstructionHtml);

        String prompt = """
                Wymagania: Edytuj załączony kod HTML zawierający przygotowanie przepisu kulinarnego. Załączona jest również pełna treść przepisu wraz ze zunifikowaną listą składników ze zmiennymi.
                UWAGA: Generujesz WYŁĄCZNIE sekcję przygotowania przepisu (kroki przygotowania oraz ewentualne istotne wskazówki i propozycje podania na końcu). Lista składników, tytuł oraz parametry ogólne są wyświetlane osobno w aplikacji nad instrukcją, dlatego NIE umieszczaj osobnej listy składników ani tytułu dania na początku HTML.

                AI ZADANIE:
                Przerób przygotowanie przepisu na krótką, przejrzystą i praktyczną instrukcję gotowania z użyciem zmiennych składników.

                ZASADY EDYCJI:

                1. SKRÓĆ PRZEPIS
                - Podziel przygotowanie dania na możliwie niewielką liczbę prostych, logicznych kroków.
                - Usuń zbędne opisy, powtórzenia, dygresje, reklamy, komentarze, pośrednie zdjęcia i niepotrzebne porady.
                - Zachowaj wszystkie istotne czynności potrzebne do przygotowania dania.
                - Zachowaj prawidłową kolejność wykonywania czynności.
                - Pisz zwięźle, prostym i naturalnym językiem.

                2. UŻYWAJ ZMIENNYCH SKŁADNIKÓW W KAŻDYM KROKU
                To najważniejsza zasada. Aplikacja dynamicznie przelicza porcje i ilości składników.
                Dlatego za każdym razem, gdy w danym kroku pojawia się składnik z listy, WSTAW ZMIENNĄ W NAWIASACH KLAMROWYCH {nazwa_zmiennej} (np. {ingredient1}, {ingredient2}).

                W aplikacji zmienne działają następująco:
                - {ingredient1} -> automatycznie wstawia przeliczoną ilość, jednostkę i nazwę składnika (np. „2400 g Kurczak”, „100 g Śmietanka”).
                - {ingredient1.name} -> wstawia samą nazwę składnika (np. „Śmietanka”).
                - {ingredient1.quantity} -> wstawia samą przeliczoną ilość i jednostkę (np. „100 g”).

                Przykłady użycia:
                - „Oczyść {ingredient1} z żyłek i pokrój na kawałki...” (zostanie wyświetlone jako „Oczyść 2400 g Kurczak z żyłek...”)
                - „Do miski dodaj {ingredient2}, {ingredient3} drobno posiekany, {ingredient4} i {ingredient5}...”
                - „Na patelni rozgrzej {ingredient6}. Dodaj {ingredient7} pokrojoną w kostkę...”
                - „Wlej {ingredient8} do patelni, wymieszaj i gotuj przez 5 minut...”

                Każdy krok ma zawierać wszystkie informacje potrzebne do wykonania opisanej czynności:
                - zmienną reprezentującą składnik (np. {ingredient1});
                - formę przygotowania, np. obrany, pokrojony w kostkę, starty lub posiekany;
                - dodatkowe informacje istotne dla przygotowania, np. temperatura, czas lub sposób obróbki.
                Użyj dokładnie nazw zmiennych podanych w sekcji --- UJEDNOLICONA LISTA SKŁADNIKÓW ZE ZMIENNYMI ---.

                3. ZACHOWAJ CZASY I PARAMETRY PRZYGOTOWANIA
                - Podaj czas smażenia, gotowania, pieczenia lub duszenia, jeśli został określony w źródle.
                - Zachowaj temperaturę piekarnika, tryb pieczenia, moc palnika i inne istotne parametry.
                - Uwzględnij ważne wskazówki, np. smażenie pod przykryciem, mieszanie, doprowadzenie do wrzenia lub sprawdzenie miękkości składników.
                - Jeśli czas zależy od efektu, np. miękkości warzyw, uwzględnij ten warunek.

                4. NIE WYMYŚLAJ BRAKUJĄCYCH INFORMACJI
                - Korzystaj przede wszystkim z informacji zawartych w załączonym HTML i pełnym przepisie.
                - Nie dopisuj niepotwierdzonych czynności, temperatur ani czasów.
                - Nie pomijaj ważnych informacji tylko dlatego, że trudno je skrócić.

                5. ZACHOWAJ NAJWAŻNIEJSZE ELEMENTY PRZYGOTOWANIA
                Jeżeli są dostępne w źródle, zachowaj w instrukcji:
                - instrukcję krok po kroku (numerowane kroki <ol><li>...</li></ol>);
                - temperatury i czasy (możesz wyróżnić pogrubieniem <strong>);
                - istotne wskazówki;
                - opcjonalne dodatki do podania.

                6. ZWRÓĆ HTML
                - Zwróć zmodyfikowany, czysty fragment HTML samej instrukcji przygotowania (bez znaczników <html>, <head>, <body> i bez bloków markdown ```html).
                - Usuń reklamy, zbędne sekcje, wstępy i nieistotną nawigację.
                - Przygotuj czytelny, estetyczny i uporządkowany układ z numerowanymi krokami (<ol><li>...</li></ol>).
                - Zachowaj poprawną strukturę HTML i polskie znaki.
                - Nie zmieniaj samego dania ani jego charakteru.

                7. SPRAWDŹ GOTOWY PRZEPIS
                Przed zakończeniem upewnij się, że:
                - wszystkie istotne czynności zostały uwzględnione;
                - kroki są ułożone we właściwej kolejności;
                - wszędzie, gdzie dodawany jest składnik z listy, użyto zmiennej w klamrach (np. {ingredient1});
                - czasy i temperatury nie zostały pominięte.

                JEDYNY WYNIK KOŃCOWY:
                HTML

                --- UJEDNOLICONA LISTA SKŁADNIKÓW ZE ZMIENNYMI ---
                %s

                --- PEŁNA TREŚĆ PRZEPISU (ŹRÓDŁO INFORMACJI) ---
                %s

                --- ZAŁĄCZONY HTML PRZYGOTOWANIA DO EDYCJI ---
                %s
                """.formatted(ingredientsSummary.toString(), fullRecipeContext, promptInstructionHtml);

        try {
            log.info("Sending recipe instructions to OpenAI for summarization (promptLength={})...", prompt.length());
            String aiResponse = instructionAIService.askAI(prompt, OpenAIService.GPT_4O_MINI, 0.1, apiKey);
            if (aiResponse != null && !aiResponse.isBlank()) {
                String cleaned = stripMarkdownCodeBlock(aiResponse);
                log.info("Received summarized recipe instructions from OpenAI (length={})", cleaned.length());
                return Jsoup.clean(cleaned, baseUri != null ? baseUri : "", safelist);
            } else {
                log.warn("OpenAI returned empty response for instruction summarization, falling back to raw HTML");
            }
        } catch (Exception e) {
            log.error("Failed to summarize recipe instructions with OpenAI: " + e.getMessage(), e);
        }
        return rawInstructionHtml;
    }

    public List<ScrapedRecipeData.ScrapedIngredientLine> unifyIngredients(
            String rawIngredientsHtml,
            List<ScrapedRecipeData.ScrapedIngredientLine> fallbackLines) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("OpenAI API key is not configured, using parsed ingredient lines without AI.");
            ensureVariableNames(fallbackLines);
            return fallbackLines;
        }

        if ((rawIngredientsHtml == null || rawIngredientsHtml.isBlank())
                && (fallbackLines == null || fallbackLines.isEmpty())) {
            ensureVariableNames(fallbackLines);
            return fallbackLines;
        }

        String existingDbIngredients = loadExistingIngredientNames();

        String fallbackSummary = fallbackLines == null ? "" : fallbackLines.stream()
                .map(ScrapedRecipeData.ScrapedIngredientLine::getOriginalText)
                .collect(Collectors.joining("\n- ", "- ", ""));

        String cleanIngredientsHtml = prepareHtmlForAiPrompt(rawIngredientsHtml);

        String prompt = """
                Wyciągnij wszystkie składniki z załączonego fragmentu HTML przepisu kulinarnego i ujednolić (zunifikuj) je tak, aby działało wyszukiwanie po składnikach/tagach oraz sumowanie w koszyku zakupowym (Shopping Cart).

                ZASADY UNIFIKACJI SKŁADNIKÓW:
                1. NAZWA SKŁADNIKA (`ingredientText`):
                   - MUSI być w mianowniku (forma podstawowa, z wielkiej litery), bez odmiany fleksyjnej i bez sposobu krojenia/przygotowania.
                   - Przykłady:
                     * "100g dyni ABC" -> `ingredientText`: "Dynia ABC", `quantity`: 100, `unitText`: "g", `originalText`: "Dynia ABC - 100 g"
                     * "400 g obranych i pokrojonych w kostkę ziemniaków" -> `ingredientText`: "Ziemniaki", `quantity`: 400, `unitText`: "g", `originalText`: "Ziemniaki - 400 g"
                     * "2 ząbki drobno posiekanego czosnku" -> `ingredientText`: "Czosnek", `quantity`: 2, `unitText`: "ząbek", `originalText`: "Czosnek - 2 ząbek"
                     * "1 płaska łyżeczka soli" -> `ingredientText`: "Sól", `quantity`: 1, `unitText`: "łyżeczka", `originalText`: "Sól - 1 łyżeczka"
                   - Jeśli na liście ISTNIEJĄCYCH SKŁADNIKÓW W BAZIE znajduje się już ten składnik (np. pod nazwą "Dynia hokkaido" lub "Ziemniak"), użyj DOKŁADNIE tej samej nazwy z bazy, aby składniki były w 100%% zunifikowane.

                2. ILOŚĆ (`quantity`) I JEDNOSTKA (`unitText`):
                   - `quantity` to liczba zmiennoprzecinkowa (np. 100, 0.5, 1.5, 2). Zamień ułamki słowne ("pół" -> 0.5, "ćwierć" -> 0.25, "półtorej" -> 1.5). Jeśli w przepisie jest zakres (np. "2-3"), podaj pierwszą lub średnią wartość liczbową.
                   - Jeśli w przepisie podano zarówno miarę domową, jak i dokładną wagę/objętość w gramach lub mililitrach (np. "2 szklanki mąki pszennej - 320 g" albo "1 średnia cebula - ok. 150 g"), jako `quantity` i `unitText` wybierz wartość w `g` / `kg` / `ml` / `l` (np. `quantity`: 320, `unitText`: "g"), aby koszyk zakupowy mógł poprawnie sumować gramatury.
                   - `unitText` MUSI być jedną z obsługiwanych jednostek:
                     "g", "kg", "dag", "ml", "l", "łyżeczka", "łyżka", "szklanka", "sztuka", "szczypta", "pęczek", "ząbek", "plasterek", "garść", "opakowanie".

                3. TEKST WYŚWIETLANY (`originalText`):
                   - Sformatuj ujednolicony zapis w postaci: "<ingredientText> - <ilość> <jednostka>" (np. "Dynia ABC - 100 g", a jeśli była dodatkowa miara domowa: "Mąka pszenna - 320 g (2 szklanki)").

                4. KATEGORIA W PRZEPISIE (`category`) I KATEGORIA SKŁADNIKA (`ingredientCategory`):
                   - `category`: jeśli przepis dzieli składniki na sekcje (np. "Składniki na ciasto", "Składniki na krem"), wpisz nazwę sekcji; w przeciwnym razie `null`.
                   - `ingredientCategory`: wybierz dokładnie jedną wartość z listy:
                     ["Nabiał", "Mięso", "Ryby", "Warzywa", "Owoce", "Przyprawy", "Zboża", "Pieczywo", "Tłuszcze", "Napoje", "Słodycze", "Słone Przekąski", "Suplementy", "Inne"].
                   - `optional`: `true`, jeśli składnik jest oznaczony jako opcjonalny / do podania, w przeciwnym razie `false`.

                5. ROZBIJANIE ZBIORCZYCH LINII I ZESTAWÓW PRZYPRAW NA OSOBNE SKŁADNIKI (BARDZO WAŻNE):
                   - W przepisach wiele przypraw lub składników bywa zapisanych razem w jednej linijce, oddzielonych średnikami, przecinkami lub słowami 'po', np.:
                     * "przyprawy: 1 płaska łyżka garam masala; po płaskiej łyżeczce imbiru, soli, cukru, kuminu; po pół płaskiej łyżeczki chili i mielonej kolendry"
                     * "przyprawy: 1 płaska łyżka garam masala; po 1/4 łyżeczki kuminu i soli; po sporej szczypcie chili i kurkumy"
                     * "do smaku: sól, świeżo mielony pieprz, szczypta ostrej papryki"
                   - W KAŻDYM takim przypadku MUSISZ utworzyć OSOBNY obiekt JSON dla KAŻDEJ wymienionej przyprawy/składnika!
                   - BEZWZGLĘDNY ZAKAZ łączenia ich w jedną pozycję lub wybierania tylko jednej czy dwóch przypraw!
                   - Zasada słowa 'po' (np. 'po płaskiej łyżeczce imbiru, soli, cukru, kuminu'):
                     Słowo 'po' oznacza, że KAŻDY ze składników wymienionych po przecinku lub słowie 'i' otrzymuje TĘ SAMĄ podaną ilość i jednostkę!
                     Np. dla 'po płaskiej łyżeczce imbiru, soli, cukru, kuminu' wygeneruj CZTERY osobne pozycje:
                       * Imbir: quantity: 1.0, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                       * Sól: quantity: 1.0, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                       * Cukier: quantity: 1.0, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                       * Kumin: quantity: 1.0, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                     Dla 'po pół płaskiej łyżeczki chili i mielonej kolendry':
                       * Chili: quantity: 0.5, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                       * Kolendra mielona: quantity: 0.5, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                     Dla '1 płaska łyżka garam masala':
                       * Garam masala: quantity: 1.0, unitText: "łyżka", ingredientCategory: "Przyprawy"
                     Dla 'po 1/4 łyżeczki kuminu i soli':
                       * Kumin: quantity: 0.25, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                       * Sól: quantity: 0.25, unitText: "łyżeczka", ingredientCategory: "Przyprawy"
                     Dla 'po sporej szczypcie chili i kurkumy':
                       * Chili: quantity: 1.0, unitText: "szczypta", ingredientCategory: "Przyprawy"
                       * Kurkuma: quantity: 1.0, unitText: "szczypta", ingredientCategory: "Przyprawy"

                Zwróć WYŁĄCZNIE poprawną tablicę JSON (bez bloków markdown ```json), gdzie każdy obiekt ma pola:
                [
                  {
                    "originalText": "Dynia ABC - 100 g",
                    "quantity": 100.0,
                    "unitText": "g",
                    "ingredientText": "Dynia ABC",
                    "category": null,
                    "ingredientCategory": "Warzywa",
                    "optional": false
                  }
                ]

                --- ISTNIEJĄCE SKŁADNIKI W BAZIE ---
                %s

                --- WSTĘPNIE WYCIĄGNIĘTE LINIE SKŁADNIKÓW ---
                %s

                --- HTML SEKCJI SKŁADNIKÓW ---
                %s
                """.formatted(existingDbIngredients, fallbackSummary, cleanIngredientsHtml);

        try {
            log.info("Sending recipe ingredients to OpenAI for extraction and unification (promptLength={})...", prompt.length());
            String aiResponse = ingredientAIService.askAI(prompt, OpenAIService.GPT_4O_MINI, 0.1, apiKey);
            if (aiResponse != null && !aiResponse.isBlank()) {
                String cleanedJson = stripMarkdownCodeBlock(aiResponse);
                List<ScrapedRecipeData.ScrapedIngredientLine> aiLines = parseIngredientLinesJson(cleanedJson);
                if (!aiLines.isEmpty()) {
                    ensureVariableNames(aiLines);
                    log.info("Successfully unified {} ingredients via OpenAI", aiLines.size());
                    return aiLines;
                }
            } else {
                log.warn("OpenAI returned empty response for ingredient unification, falling back to local parser");
            }
        } catch (Exception e) {
            log.error("Failed to unify ingredients with OpenAI, falling back to local parser: " + e.getMessage(), e);
        }
        ensureVariableNames(fallbackLines);
        return fallbackLines;
    }

    private void ensureVariableNames(List<ScrapedRecipeData.ScrapedIngredientLine> lines) {
        if (lines == null) return;
        for (int i = 0; i < lines.size(); i++) {
            ScrapedRecipeData.ScrapedIngredientLine line = lines.get(i);
            if (line != null && (line.getVariableName() == null || line.getVariableName().isBlank())) {
                line.setVariableName("ingredient" + (i + 1));
            }
        }
    }

    private List<ScrapedRecipeData.ScrapedIngredientLine> parseIngredientLinesJson(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode arrayNode = root;
        if (root.isObject()) {
            for (JsonNode child : root) {
                if (child.isArray()) {
                    arrayNode = child;
                    break;
                }
            }
        }
        List<ScrapedRecipeData.ScrapedIngredientLine> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                String ingredientText = textOrNull(node, "ingredientText");
                if (ingredientText == null || ingredientText.isBlank()) {
                    continue;
                }
                Double quantity = parseDoubleNode(node.get("quantity"));
                String unitText = textOrNull(node, "unitText");
                String originalText = textOrNull(node, "originalText");
                String category = textOrNull(node, "category");
                String ingredientCategory = textOrNull(node, "ingredientCategory");
                Boolean optional = node.hasNonNull("optional") && node.get("optional").asBoolean(false);
                String variableName = textOrNull(node, "variableName");
                if (variableName == null || variableName.isBlank()) {
                    variableName = "ingredient" + (result.size() + 1);
                }

                result.add(ScrapedRecipeData.ScrapedIngredientLine.builder()
                        .originalText(originalText != null ? originalText : ingredientText)
                        .quantity(quantity)
                        .unitText(unitText)
                        .ingredientText(ingredientText)
                        .category(category)
                        .ingredientCategory(ingredientCategory)
                        .optional(optional)
                        .variableName(variableName)
                        .build());
            }
        }
        return result;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String val = node.get(field).asText(null);
        return (val == null || val.isBlank() || "null".equalsIgnoreCase(val.trim())) ? null : val.trim();
    }

    private Double parseDoubleNode(JsonNode qNode) {
        if (qNode == null || qNode.isNull()) {
            return null;
        }
        if (qNode.isNumber()) {
            return qNode.asDouble();
        }
        String text = qNode.asText("");
        Matcher m = NUMBER_EXTRACTOR.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", "."));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String prepareHtmlForAiPrompt(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parseBodyFragment(html);
        doc.select("img, picture, figure, svg, noscript, script, style, iframe").remove();
        return Jsoup.clean(doc.body().html(), PROMPT_HTML_SAFELIST);
    }

    private String loadExistingIngredientNames() {
        if (ingredientRepository == null) {
            return "";
        }
        try {
            List<String> names = ingredientRepository.findAll().stream()
                    .filter(i -> i != null && !Boolean.TRUE.equals(i.isDeleted()) && i.getName() != null)
                    .map(Ingredient::getName)
                    .distinct()
                    .limit(500)
                    .collect(Collectors.toList());
            return String.join(", ", names);
        } catch (Exception e) {
            log.warn("Could not load existing ingredients for AI prompt context: " + e.getMessage());
            return "";
        }
    }

    private String stripMarkdownCodeBlock(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            } else {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }
        return cleaned.trim();
    }
}
