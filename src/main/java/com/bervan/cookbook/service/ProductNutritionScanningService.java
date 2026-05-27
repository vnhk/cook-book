package com.bervan.cookbook.service;

import com.bervan.common.service.BaseScanningService;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.logging.JsonLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ProductNutritionScanningService extends BaseScanningService {
    private static final JsonLogger log = JsonLogger.getLogger(ProductNutritionScanningService.class, "cook-book");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductNutritionScanningService(@Value("${openai.api.key}") String apiKey) {
        super("You are an expert nutrition label parser.", apiKey);
    }

    public Ingredient scanNutritionLabel(String base64Image) throws IOException {

        String prompt = """
                Analyze the attached product nutrition table / ingredients label image.
                
                Extract nutritional values PER 100g of product.
                
                Return the result STRICTLY as a valid minified raw JSON object.
                Do NOT wrap the JSON in markdown code blocks.
                
                The JSON object must contain exactly these fields:
                - kcalPer100g: Double (kcal per 100g)
                - proteinPer100g: Double (protein per 100g)
                - fatPer100g: Double (total fat per 100g, NOT saturated split)
                - carbsPer100g: Double (carbohydrates per 100g)
                - fiberPer100g: Double (fiber per 100g)
                
                Rules:
                - Extract ONLY values for 100g.
                - Ignore serving size values.
                - Fat must be TOTAL fat only (ignore saturated/unsaturated breakdown).
                - If fiber is missing, return 0.
                - Numbers must not contain units.
                - Return ONLY valid JSON.
                """;

        log.info("Sending nutrition label image to OpenAI for analysis...");

        String response = super.askAIWithImage(base64Image, prompt);

        if (response == null) {
            log.error("Failed to receive response from OpenAI.");
            return null;
        }

        log.info("OpenAI nutrition response successfully received.");

        try {
            return objectMapper.readValue(response, Ingredient.class);
        } catch (Exception e) {
            log.error("Failed to parse nutrition JSON response: {}", response, e);
            return null;
        }
    }
}