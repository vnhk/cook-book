package com.bervan.cookbook.service;

import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.model.IngredientAlias;
import com.bervan.cookbook.repository.IngredientAliasRepository;
import com.bervan.cookbook.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IngredientNormalizationEngine {
    private static final Set<String> NOISE_WORDS = Set.of(
            "do", "na", "z", "ze", "od", "w", "i", "lub", "albo", "ok", "okolo",
            "swiezy", "swieza", "swieze", "swiezych", "swiezej",
            "duzy", "duza", "duze", "duzych", "maly", "mala", "male", "malych",
            "sredni", "srednia", "srednie", "srednich", "plaska", "plaskie", "czubata", "czubate",
            "obrany", "obrana", "obrane", "obranych", "obranej",
            "pokrojony", "pokrojona", "pokrojone", "pokrojonych", "pokrojonej", "kostke",
            "posiekany", "posiekana", "posiekane", "posiekanych", "posiekanej", "drobno",
            "starty", "starta", "starte", "startych", "startego", "opcjonalnie", "podania"
    );

    private static final List<String> POLISH_SUFFIXES = List.of(
            "ami", "ach", "om", "ow", "ie", "em", "ej", "ego", "ych", "im", "ym",
            "ka", "ki", "ek", "ku", "ce", "a", "i", "y", "u", "e", "o"
    );

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;

    public IngredientNormalizationEngine(IngredientRepository ingredientRepository,
                                         IngredientAliasRepository aliasRepository) {
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
    }

    public Optional<Ingredient> normalize(String inputText) {
        if (inputText == null || inputText.isBlank()) {
            return Optional.empty();
        }

        String cleaned = inputText.trim().toLowerCase();

        // 1. Exact match on name
        Optional<Ingredient> byName = ingredientRepository.findByNameIgnoreCase(cleaned);
        if (byName.isPresent() && !Boolean.TRUE.equals(byName.get().isDeleted())) {
            return byName;
        }

        // 2. Exact match on alias
        Optional<IngredientAlias> byAlias = aliasRepository.findByAliasNameIgnoreCase(cleaned);
        if (byAlias.isPresent() && !Boolean.TRUE.equals(byAlias.get().isDeleted())
                && byAlias.get().getIngredient() != null
                && !Boolean.TRUE.equals(byAlias.get().getIngredient().isDeleted())) {
            return Optional.of(byAlias.get().getIngredient());
        }

        // 3. Fuzzy matching
        List<IngredientMatch> candidates = findCandidates(inputText, 1);
        if (!candidates.isEmpty() && candidates.get(0).getScore() >= 0.5) {
            return Optional.of(candidates.get(0).getIngredient());
        }

        return Optional.empty();
    }

    public List<IngredientMatch> findCandidates(String inputText, int maxResults) {
        if (inputText == null || inputText.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> inputTokens = tokenize(inputText);
        if (inputTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<IngredientMatch> matches = new ArrayList<>();

        // Search ingredients by token and stemmed prefix
        Set<Ingredient> candidateIngredients = new HashSet<>();
        for (String token : inputTokens) {
            if (token.length() >= 2) {
                candidateIngredients.addAll(ingredientRepository.findByNameContainingIgnoreCase(token));
                aliasRepository.findByAliasNameContainingIgnoreCase(token).stream()
                        .map(IngredientAlias::getIngredient)
                        .forEach(candidateIngredients::add);

                String stem = stemPolish(token);
                if (stem.length() >= 3 && !stem.equals(token)) {
                    candidateIngredients.addAll(ingredientRepository.findByNameContainingIgnoreCase(stem));
                    aliasRepository.findByAliasNameContainingIgnoreCase(stem).stream()
                            .map(IngredientAlias::getIngredient)
                            .forEach(candidateIngredients::add);
                }
            }
        }

        for (Ingredient ingredient : candidateIngredients) {
            if (ingredient == null || Boolean.TRUE.equals(ingredient.isDeleted())) {
                continue;
            }

            // Score against name
            double nameScore = jaccardSimilarity(inputTokens, tokenize(ingredient.getName()));
            String matchedVia = "fuzzy-name";

            // Score against aliases
            if (ingredient.getAliases() != null) {
                for (IngredientAlias alias : ingredient.getAliases()) {
                    if (Boolean.TRUE.equals(alias.isDeleted())) continue;
                    double aliasScore = jaccardSimilarity(inputTokens, tokenize(alias.getAliasName()));
                    if (aliasScore > nameScore) {
                        nameScore = aliasScore;
                        matchedVia = "fuzzy-alias";
                    }
                }
            }

            if (nameScore > 0) {
                IngredientMatch match = new IngredientMatch();
                match.setIngredient(ingredient);
                match.setScore(nameScore);
                match.setMatchedVia(matchedVia);
                matches.add(match);
            }
        }

        matches.sort(Comparator.comparingDouble(IngredientMatch::getScore).reversed());
        return matches.stream().limit(maxResults).collect(Collectors.toList());
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Collections.emptySet();
        String normalized = stripDiacritics(text.trim().toLowerCase());
        return Arrays.stream(normalized.split("[\\s,()\\-–/]+"))
                .filter(t -> !t.isBlank())
                .filter(t -> t.length() >= 2)
                .filter(t -> !t.matches("\\d+"))
                .filter(t -> !NOISE_WORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private String stemPolish(String token) {
        if (token == null || token.length() <= 3) {
            return token;
        }
        String res = token;
        for (String suffix : POLISH_SUFFIXES) {
            if (res.endsWith(suffix) && res.length() - suffix.length() >= 3) {
                res = res.substring(0, res.length() - suffix.length());
                break;
            }
        }
        // Handle consonant alternations like czosnku -> czosnk vs czosnek -> czosn
        if (res.endsWith("nk") && res.length() >= 5) {
            res = res.substring(0, res.length() - 1);
        }
        return res;
    }

    private String stripDiacritics(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replace("ł", "l").replace("Ł", "L");
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> stemsA = a.stream().map(this::stemPolish).collect(Collectors.toSet());
        Set<String> stemsB = b.stream().map(this::stemPolish).collect(Collectors.toSet());
        Set<String> intersection = new HashSet<>(stemsA);
        intersection.retainAll(stemsB);
        Set<String> union = new HashSet<>(stemsA);
        union.addAll(stemsB);
        return (double) intersection.size() / union.size();
    }

    public static class IngredientMatch {
        private Ingredient ingredient;
        private double score;
        private String matchedVia;

        public Ingredient getIngredient() {
            return ingredient;
        }

        public void setIngredient(Ingredient ingredient) {
            this.ingredient = ingredient;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getMatchedVia() {
            return matchedVia;
        }

        public void setMatchedVia(String matchedVia) {
            this.matchedVia = matchedVia;
        }
    }
}
