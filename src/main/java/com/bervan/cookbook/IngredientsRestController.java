package com.bervan.cookbook;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.service.IngredientService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/cook-book/ingredients")
public class IngredientsRestController extends BaseOwnedController {


    protected IngredientsRestController(IngredientService service, BervanDTOMapper mapper, EntityConfigValidator validator) {
        super(service, mapper, validator, "Ingredient");
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
        return super.exportAll(allParams, IngredientDto.class, "stock-alerts", Ingredient.class);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importData(@RequestParam("file") MultipartFile file) {
        return super.importAll(file, IngredientDto.class);
    }
}
