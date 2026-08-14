package com.bervan.cookbook;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.ImportResult;
import com.bervan.common.controller.ValidationErrorResponse;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.service.IngredientService;
import com.bervan.cookbook.service.ProductNutritionScanningService;
import com.bervan.logging.JsonLogger;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cook-book/ingredients")
public class IngredientsRestController extends BaseOwnedController {
    private static final JsonLogger log = JsonLogger.getLogger(IngredientsRestController.class, "cook-book");
    private final ProductNutritionScanningService productNutritionScanningService;

    protected IngredientsRestController(IngredientService service, BervanDTOMapper mapper, EntityConfigValidator validator, ProductNutritionScanningService productNutritionScanningService) {
        super(service, mapper, validator, "Ingredient");
        this.productNutritionScanningService = productNutritionScanningService;
    }

    @GetMapping
    public ResponseEntity<Page<IngredientDto>> getIngredients(
            @RequestParam MultiValueMap<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size
    ) {
        return super.search(allParams, page, size, IngredientDto.class, Ingredient.class);

    }

    @PostMapping
    public ResponseEntity<IngredientDto> createIngredient(@RequestBody IngredientDto req) {
        return super.create(req);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientDto> updateIngredient(@PathVariable UUID id,
                                                          @RequestBody IngredientDto req) {
        req.setId(id);
        return super.update(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIngredient(@PathVariable UUID id) {
        return super.delete(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam MultiValueMap<String, String> allParams) {
        return super.exportAll(allParams, IngredientDto.class, "ingredients", Ingredient.class);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importData(@RequestParam("file") MultipartFile file) {
        return super.importAll(file, IngredientDto.class);
    }

    @PostMapping("/scan-nutrition-table")
    public ResponseEntity<?> scanNutritionTable(@RequestBody ScanNutritionRequest req) {
        if (req.base64Image == null || req.base64Image.isBlank()) {
            return ResponseEntity.badRequest().body(new ValidationErrorResponse(
                    List.of(new EntityConfigValidator.FieldError("base64Image", "Image data is required"))
            ));
        }

        Ingredient parsed = null;
        try {
            parsed = productNutritionScanningService.scanNutritionLabel(req.base64Image);
        } catch (IOException e) {
            log.error("Failed to scan receipt", e);
        }

        if (parsed == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(super.map(parsed, IngredientDto.class));
    }
}
