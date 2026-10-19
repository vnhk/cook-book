package com.bervan.cookbook.service;

import com.bervan.common.service.OpenAIService;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.model.ScrapedRecipeData;
import com.bervan.cookbook.repository.IngredientRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipeAIScraperService {
    private static final Logger log = LoggerFactory.getLogger(RecipeAIScraperService.class);

    private static final String AI_INSTRUCTION_SYSTEM_PROMPT =
            "Jesteś doświadczonym redaktorem kulinarnym specjalizującym się w tworzeniu krótkich, " +
            "przejrzystych i praktycznych instrukcji gotowania w formacie HTML.";

    private static final String AI_INGREDIENT_SYSTEM_PROMPT =
            "Jesteś ekspertem kulinarnym i analitykiem danych. Twoim zadaniem jest ekstrakcja i " +
            "ujednolicenie (unifikacja) listy składników z przepisu kulinarnego do czystego formatu JSON.";

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
            for (ScrapedRecipeData.ScrapedIngredientLine ing : unifiedIngredients) {
                ingredientsSummary.append("- ").append(ing.getOriginalText());
                if (ing.getCategory() != null && !ing.getCategory().isBlank()) {
                    ingredientsSummary.append(" [").append(ing.getCategory()).append("]");
                }
                ingredientsSummary.append("\n");
            }
        }

        String prompt = """
                Wymagania: Edytuj załączony kod HTML zawierający przygotowanie przepisu kulinarnego. Załączona jest również pełna treść przepisu wraz z listą składników, która ma służyć jako źródło informacji.
                UWAGA: Generujesz WYŁĄCZNIE sekcję przygotowania przepisu (kroki przygotowania oraz ewentualne istotne wskazówki i propozycje podania na końcu). Lista składników, tytuł oraz parametry ogólne są wyświetlane osobno w aplikacji nad instrukcją, dlatego NIE umieszczaj osobnej listy składników ani tytułu dania na początku HTML.

                AI ZADANIE:
                Przerób przygotowanie przepisu na krótką, przejrzystą i praktyczną instrukcję gotowania, tak aby można było przygotować danie, wykonując proste kroki bez konieczności ciągłego sprawdzania listy składników lub czytania długiego opisu.

                ZASADY EDYCJI:

                1. SKRÓĆ PRZEPIS
                - Podziel przygotowanie dania na możliwie niewielką liczbę prostych, logicznych kroków.
                - Usuń zbędne opisy, powtórzenia, dygresje, reklamy, komentarze, pośrednie zdjęcia i niepotrzebne porady.
                - Zachowaj wszystkie istotne czynności potrzebne do przygotowania dania.
                - Zachowaj prawidłową kolejność wykonywania czynności.
                - Pisz zwięźle, prostym i naturalnym językiem.

                2. PODAWAJ PEŁNE INFORMACJE O SKŁADNIKACH W KAŻDYM KROKU
                To najważniejsza zasada.
                Za każdym razem, gdy w danym kroku pojawia się składnik, podaj wszystkie informacje niezbędne do jego użycia:
                - dokładną ilość, np. 200 g;
                - formę przygotowania, np. obrany, pokrojony w kostkę, starty lub posiekany;
                - dodatkowe informacje istotne dla przygotowania, np. temperatura, rodzaj produktu lub sposób wcześniejszej obróbki, jeśli wynikają z przepisu.

                Nie pisz ogólnie:
                „Dodaj ziemniaki do garnka”.
                Napisz:
                „Dodaj 400 g obranych i pokrojonych w kostkę ziemniaków”.

                Nie pisz:
                „Dodaj czosnek i przyprawy”.
                Napisz:
                „Dodaj 2 ząbki drobno posiekanego czosnku i 1 płaską łyżeczkę soli”.

                Każdy krok ma zawierać wszystkie informacje potrzebne do wykonania opisanej czynności bez konieczności szukania ich na liście składników.
                Jeżeli składnik jest używany w kilku oddzielnych etapach, podaj odpowiednią ilość w każdym z tych etapów, o ile źródło pozwala ją ustalić. Nie dziel samodzielnie ilości składnika, jeśli nie wynika to z przepisu.

                3. ZACHOWAJ CZASY I PARAMETRY PRZYGOTOWANIA
                - Podaj czas smażenia, gotowania, pieczenia lub duszenia, jeśli został określony w źródle.
                - Zachowaj temperaturę piekarnika, tryb pieczenia, moc palnika i inne istotne parametry.
                - Uwzględnij ważne wskazówki, np. smażenie pod przykryciem, mieszanie, doprowadzenie do wrzenia lub sprawdzenie miękkości składników.
                - Jeśli czas zależy od efektu, np. miękkości warzyw, uwzględnij ten warunek.

                4. NIE WYMYŚLAJ BRAKUJĄCYCH INFORMACJI
                - Korzystaj przede wszystkim z informacji zawartych w załączonym HTML i pełnym przepisie.
                - Nie dopisuj niepotwierdzonych ilości, czasów ani czynności.
                - Jeśli ilość składnika jest podana orientacyjnie, zachowaj tę informację.
                - Jeśli źródła podają sprzeczne dane, nie rozstrzygaj samodzielnie, która wartość jest prawidłowa. Wybierz najbardziej użyteczną formę przedstawienia informacji, a rozbieżność krótko zaznacz w przepisie lub w uwadze.
                - Nie pomijaj ważnych informacji tylko dlatego, że trudno je skrócić.

                5. ZACHOWAJ NAJWAŻNIEJSZE ELEMENTY PRZYGOTOWANIA
                Jeżeli są dostępne w źródle, zachowaj w instrukcji:
                - instrukcję krok po kroku (numerowane kroki <ol><li>...</li></ol>);
                - temperatury i czasy (możesz wyróżnić pogrubieniem <strong>);
                - istotne wskazówki;
                - opcjonalne dodatki do podania.

                6. ZWRÓĆ HTML
                - Zwróć zmodyfikowany, czysty fragment HTML samej instrukcji przygotowania (bez znaczników <html>, <head>, <body> i bez bloków markdown ```html).
                - Usuń elementy strony, które nie są potrzebne do przygotowania potrawy, np. reklamy, zbędne sekcje, długie wstępy i nieistotne elementy nawigacji.
                - Przygotuj czytelny, estetyczny i uporządkowany układ z numerowanymi krokami (<ol><li>...</li></ol>).
                - Zadbaj o czytelność na komputerze i telefonie.
                - Zachowaj poprawną strukturę HTML i polskie znaki.
                - Nie zmieniaj samego dania ani jego charakteru.

                7. SPRAWDŹ GOTOWY PRZEPIS
                Przed zakończeniem upewnij się, że:
                - wszystkie istotne czynności zostały uwzględnione;
                - kroki są ułożone we właściwej kolejności;
                - ilości składników zgadzają się ze źródłem;
                - każdy krok zawiera komplet informacji o składnikach używanych na danym etapie;
                - czasy i temperatury nie zostały pominięte;
                - nie dodano niepotwierdzonych informacji;
                - przepis można wykonać bez konieczności ciągłego wracania do oryginalnego tekstu.

                JEDYNY WYNIK KOŃCOWY:
                HTML

                --- UJEDNOLICONA LISTA SKŁADNIKÓW ---
                %s

                --- PEŁNA TREŚĆ PRZEPISU (ŹRÓDŁO INFORMACJI) ---
                %s

                --- ZAŁĄCZONY HTML PRZYGOTOWANIA DO EDYCJI ---
                %s
                """.formatted(ingredientsSummary.toString(), fullRecipeContext, rawInstructionHtml);

        try {
            log.info("Sending recipe instructions to OpenAI for summarization...");
            String aiResponse = instructionAIService.askAI(prompt, OpenAIService.GPT_4O_MINI, 0.1, apiKey);
            if (aiResponse != null && !aiResponse.isBlank()) {
                String cleaned = stripMarkdownCodeBlock(aiResponse);
                return Jsoup.clean(cleaned, baseUri != null ? baseUri : "", safelist);
            }
        } catch (Exception e) {
            log.error("Failed to summarize recipe instructions with OpenAI, falling back to raw HTML", e);
        }
        return rawInstructionHtml;
    }

    public List<ScrapedRecipeData.ScrapedIngredientLine> unifyIngredients(
            String rawIngredientsHtml,
            List<ScrapedRecipeData.ScrapedIngredientLine> fallbackLines) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("OpenAI API key is not configured, using parsed ingredient lines without AI.");
            return fallbackLines;
        }

        if ((rawIngredientsHtml == null || rawIngredientsHtml.isBlank())
                && (fallbackLines == null || fallbackLines.isEmpty())) {
            return fallbackLines;
        }

        String existingDbIngredients = loadExistingIngredientNames();

        String fallbackSummary = fallbackLines == null ? "" : fallbackLines.stream()
                .map(ScrapedRecipeData.ScrapedIngredientLine::getOriginalText)
                .collect(Collectors.joining("\n- ", "- ", ""));

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
                """.formatted(existingDbIngredients, fallbackSummary, rawIngredientsHtml != null ? rawIngredientsHtml : "");

        try {
            log.info("Sending recipe ingredients to OpenAI for extraction and unification...");
            String aiResponse = ingredientAIService.askAI(prompt, OpenAIService.GPT_4O_MINI, 0.1, apiKey);
            if (aiResponse != null && !aiResponse.isBlank()) {
                String cleanedJson = stripMarkdownCodeBlock(aiResponse);
                List<ScrapedRecipeData.ScrapedIngredientLine> aiLines = objectMapper.readValue(
                        cleanedJson,
                        new TypeReference<List<ScrapedRecipeData.ScrapedIngredientLine>>() {}
                );
                if (aiLines != null && !aiLines.isEmpty()) {
                    return aiLines;
                }
            }
        } catch (Exception e) {
            log.error("Failed to unify ingredients with OpenAI, falling back to local parser", e);
        }
        return fallbackLines;
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
            log.warn("Could not load existing ingredients for AI prompt context", e);
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
