package com.bervan.cookbook.view;

import com.bervan.common.component.BervanButton;
import com.bervan.common.component.InlineEditableField;
import com.bervan.common.component.WysiwygTextArea;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractPageView;
import com.bervan.cookbook.model.*;
import com.bervan.cookbook.service.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;

import java.util.*;
import java.util.stream.Collectors;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractRecipeDetailView extends AbstractPageView implements HasUrlParameter<String> {
    public static final String ROUTE_NAME = "/cook-book/recipe-details/";

    private final RecipeService recipeService;
    private final IngredientService ingredientService;
    private final ShoppingCartService shoppingCartService;
    private final UnitConversionEngine unitConversionEngine;
    private final BervanViewConfig bervanViewConfig;
    private final CookBookPageLayout cookBookPageLayout = new CookBookPageLayout(ROUTE_NAME);
    private List<String> allAvailableTags = new ArrayList<>();

    public AbstractRecipeDetailView(RecipeService recipeService,
                                     IngredientService ingredientService,
                                     ShoppingCartService shoppingCartService,
                                     UnitConversionEngine unitConversionEngine,
                                     BervanViewConfig bervanViewConfig) {
        this.recipeService = recipeService;
        this.ingredientService = ingredientService;
        this.shoppingCartService = shoppingCartService;
        this.unitConversionEngine = unitConversionEngine;
        this.bervanViewConfig = bervanViewConfig;
        add(cookBookPageLayout);

        try {
            allAvailableTags = recipeService.loadAllTags();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void setParameter(BeforeEvent event, String s) {
        String recipeId = event.getRouteParameters().get("___url_parameter").orElse(UUID.randomUUID().toString());
        init(recipeId);
    }

    private void init(String recipeId) {
        getChildren().filter(c -> c != cookBookPageLayout).toList().forEach(this::remove);

        Optional<Recipe> recipeOpt = recipeService.loadById(UUID.fromString(recipeId));
        if (recipeOpt.isEmpty()) {
            showErrorNotification("Recipe does not exist!");
            return;
        }

        Recipe recipe = recipeOpt.get();
        cookBookPageLayout.updateButtonText(ROUTE_NAME, recipe.getName());
        setWidthFull();

        // === Main Image ===
        add(buildImageSection(recipe));

        // === Header Card ===
        add(buildHeaderSection(recipe));

        // === Metadata Grid ===
        add(buildMetadataSection(recipe));

        // === Ingredients Section ===
        add(buildIngredientsSection(recipe));

        // === Instructions Section ===
        add(buildInstructionSection(recipe));

        // === Add to Cart ===
        add(buildAddToCartSection(recipe));
    }

    private Component buildImageSection(Recipe recipe) {
        Div section = new Div();
        section.getStyle().set("width", "100%");

        if (recipe.getMainImageUrl() != null && !recipe.getMainImageUrl().isBlank()) {
            Image img = new Image(recipe.getMainImageUrl(), recipe.getName());
            img.addClassName("cb-recipe-main-image");
            img.getStyle().set("cursor", "pointer");
            img.addClickListener(e -> openImageUrlDialog(recipe));
            section.add(img);
        } else {
            BervanButton addImageBtn = new BervanButton(new Icon(VaadinIcon.CAMERA),
                    e -> openImageUrlDialog(recipe));
            addImageBtn.getElement().setAttribute("title", "Add Image");
            addImageBtn.addClassName("bervan-icon-btn");
            section.add(addImageBtn);
        }

        return section;
    }

    private void openImageUrlDialog(Recipe recipe) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Recipe Image URL");
        dialog.setWidth("500px");

        TextField urlField = new TextField("Image URL");
        urlField.setWidthFull();
        urlField.setValue(recipe.getMainImageUrl() != null ? recipe.getMainImageUrl() : "");
        urlField.setPlaceholder("https://...");

        BervanButton saveBtn = new BervanButton("Save", e -> {
            String url = urlField.getValue();
            recipe.setMainImageUrl(url != null && !url.isBlank() ? url : null);
            recipeService.save(recipe);
            dialog.close();
            init(recipe.getId().toString());
        });

        dialog.add(new VerticalLayout(urlField));
        dialog.getFooter().add(saveBtn);
        dialog.open();
    }

    private Component buildHeaderSection(Recipe recipe) {
        Div headerCard = new Div();
        headerCard.addClassName("bervan-glass-card");
        headerCard.addClassName("pm-section");
        headerCard.getStyle().set("width", "100%");

        HorizontalLayout headerContent = new HorizontalLayout();
        headerContent.setWidthFull();
        headerContent.setAlignItems(FlexComponent.Alignment.CENTER);
        headerContent.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        InlineEditableField nameField = new InlineEditableField(
                "Name", recipe.getName(), InlineEditableField.FieldType.TEXT,
                val -> {
                    recipe.setName(val != null ? val.toString() : "");
                    recipeService.save(recipe);
                    cookBookPageLayout.updateButtonText(ROUTE_NAME, recipe.getName());
                }
        );
        nameField.getStyle().set("flex-grow", "1");

        Icon favoriteIcon = CookBookUIHelper.createFavoriteIcon(recipe.getFavorite());
        favoriteIcon.getStyle().set("cursor", "pointer");
        favoriteIcon.addClickListener(e -> {
            recipeService.toggleFavorite(recipe.getId());
            init(recipe.getId().toString());
        });

        headerContent.add(nameField, favoriteIcon);
        headerCard.add(headerContent);

        // Tags (inline editable multi-select)
        InlineEditableField tagsField = new InlineEditableField(
                "Tags", recipe.getTags(), InlineEditableField.FieldType.MULTI_SELECT,
                allAvailableTags,
                val -> {
                    @SuppressWarnings("unchecked")
                    List<String> tags = val instanceof List ? (List<String>) val : new ArrayList<>();
                    recipe.setTags(tags);
                    recipeService.save(recipe);
                    try {
                        allAvailableTags = recipeService.loadAllTags();
                    } catch (Exception ignored) {
                    }
                    init(recipe.getId().toString());
                }
        );
        headerCard.add(tagsField);

        return headerCard;
    }

    private Component buildMetadataSection(Recipe recipe) {
        Div section = new Div();
        section.addClassName("bervan-glass-card");
        section.addClassName("pm-section");
        section.getStyle().set("width", "100%");

        Div grid = new Div();
        grid.addClassName("cb-metadata-grid");

        grid.add(new InlineEditableField("Prep Time (min)", recipe.getPrepTime(),
                InlineEditableField.FieldType.NUMBER, val -> {
            recipe.setPrepTime(val != null ? ((Double) val).intValue() : null);
            recipeService.save(recipe);
            init(recipe.getId().toString());
        }));

        grid.add(new InlineEditableField("Cook Time (min)", recipe.getCookTime(),
                InlineEditableField.FieldType.NUMBER, val -> {
            recipe.setCookTime(val != null ? ((Double) val).intValue() : null);
            recipeService.save(recipe);
            init(recipe.getId().toString());
        }));

        // Total Time - read only (auto-calculated)
        Div totalTimeItem = new Div();
        Span totalLabel = new Span("Total Time");
        totalLabel.addClassName("field-label");
        Span totalValue = new Span(recipe.getTotalTime() != null ? recipe.getTotalTime() + " min" : "—");
        totalValue.addClassName("field-value");
        totalTimeItem.add(totalLabel, new Div(totalValue));
        grid.add(totalTimeItem);

        grid.add(new InlineEditableField("Servings", recipe.getServings(),
                InlineEditableField.FieldType.NUMBER, val -> {
            recipe.setServings(val != null ? ((Double) val).intValue() : null);
            recipeService.save(recipe);
        }));

        grid.add(new InlineEditableField("Calories", recipe.getTotalCalories(),
                InlineEditableField.FieldType.NUMBER, val -> {
            recipe.setTotalCalories(val != null ? ((Double) val).intValue() : null);
            recipeService.save(recipe);
        }));

        // Rating - inline editable average + count + visual stars
        Div ratingItem = new Div();
        ratingItem.add(new InlineEditableField("Rating", recipe.getAverageRating(),
                InlineEditableField.FieldType.NUMBER, val -> {
            recipe.setAverageRating(val != null ? (Double) val : null);
            recipeService.save(recipe);
            init(recipe.getId().toString());
        }));
        ratingItem.add(new InlineEditableField("Rating Count", recipe.getRatingCount(),
                InlineEditableField.FieldType.NUMBER, val -> {
            recipe.setRatingCount(val != null ? ((Double) val).intValue() : null);
            recipeService.save(recipe);
            init(recipe.getId().toString());
        }));
        ratingItem.add(CookBookUIHelper.createRatingStars(recipe.getAverageRating(), recipe.getRatingCount()));
        grid.add(ratingItem);

        section.add(grid);
        return section;
    }

    private Component buildIngredientsSection(Recipe recipe) {
        Div section = new Div();
        section.addClassName("bervan-glass-card");
        section.addClassName("pm-section");
        section.getStyle().set("width", "100%");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H3 title = new H3("Ingredients");
        BervanButton addBtn = new BervanButton("+ Add");
        addBtn.addClickListener(e -> openAddIngredientDialog(recipe));

        header.add(title, addBtn);
        section.add(header);

        List<RecipeIngredient> ingredients = recipe.getRecipeIngredients() != null
                ? recipe.getRecipeIngredients().stream()
                .filter(ri -> !Boolean.TRUE.equals(ri.isDeleted()))
                .sorted(Comparator.comparing(ri -> ri.getIngredient().getName()))
                .toList()
                : Collections.emptyList();

        // Group by category
        Map<String, List<RecipeIngredient>> grouped = ingredients.stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getCategory() != null ? ri.getCategory() : "Składniki",
                        LinkedHashMap::new, Collectors.toList()));

        if (grouped.size() <= 1) {
            section.add(buildIngredientGrid(recipe, ingredients));
        } else {
            for (Map.Entry<String, List<RecipeIngredient>> entry : grouped.entrySet()) {
                Details details = new Details(
                        entry.getKey() + " (" + entry.getValue().size() + ")",
                        buildIngredientGrid(recipe, entry.getValue()));
                details.setOpened(true);
                details.setWidthFull();
                section.add(details);
            }
        }

        return section;
    }

    private Grid<RecipeIngredient> buildIngredientGrid(Recipe recipe, List<RecipeIngredient> ingredients) {
        Grid<RecipeIngredient> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.setItems(ingredients);

        grid.addColumn(ri -> ri.getQuantity() != null
                ? unitConversionEngine.formatQuantity(ri.getQuantity(), ri.getUnit() != null ? ri.getUnit() : CulinaryUnit.PIECE)
                : "-").setHeader("Quantity").setWidth("120px");

        grid.addColumn(ri -> ri.getIngredient().getName()).setHeader("Ingredient").setFlexGrow(1);

        grid.addColumn(ri -> ri.getOriginalText() != null ? ri.getOriginalText() : "")
                .setHeader("Original").setFlexGrow(1);

        grid.addColumn(new ComponentRenderer<>(ri -> {
            if (Boolean.TRUE.equals(ri.getOptional())) {
                Span opt = new Span("optional");
                opt.addClassName("cb-optional-ingredient");
                return opt;
            }
            return new Span();
        })).setHeader("").setWidth("80px");

        grid.addColumn(new ComponentRenderer<>(ri -> {
            Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
            removeBtn.addClassName("relation-remove-btn");
            removeBtn.addClickListener(e -> {
                recipe.getRecipeIngredients().remove(ri);
                recipeService.save(recipe);
                init(recipe.getId().toString());
            });
            return removeBtn;
        })).setHeader("").setWidth("50px");

        return grid;
    }

    private void openAddIngredientDialog(Recipe recipe) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Ingredient");
        dialog.setWidth("500px");

        VerticalLayout layout = new VerticalLayout();

        ComboBox<Ingredient> ingredientCombo = new ComboBox<>("Ingredient");
        ingredientCombo.setWidthFull();
        ingredientCombo.setItemLabelGenerator(Ingredient::getName);
        ingredientCombo.setItems(query -> {
            String filter = query.getFilter().orElse("");
            int offset = query.getOffset();
            int limit = query.getLimit();
            return ingredientService.searchByText(filter.isBlank() ? "a" : filter, offset, limit).stream();
        });
        ingredientCombo.setAllowCustomValue(true);
        ingredientCombo.addCustomValueSetListener(e -> {
            Ingredient newIng = ingredientService.findOrCreateByName(e.getDetail());
            ingredientCombo.setValue(newIng);
        });

        NumberField quantityField = new NumberField("Quantity");
        quantityField.setWidthFull();

        ComboBox<CulinaryUnit> unitCombo = new ComboBox<>("Unit");
        unitCombo.setItems(CulinaryUnit.values());
        unitCombo.setItemLabelGenerator(CulinaryUnit::getDisplayName);
        unitCombo.setValue(CulinaryUnit.GRAM);
        unitCombo.setWidthFull();

        // Category with autocomplete from existing categories in this recipe
        ComboBox<String> categoryCombo = new ComboBox<>("Category");
        categoryCombo.setWidthFull();
        categoryCombo.setAllowCustomValue(true);
        categoryCombo.setPlaceholder("e.g., Na ciasto, Na farsz");
        List<String> existingCategories = recipe.getRecipeIngredients() != null
                ? recipe.getRecipeIngredients().stream()
                .map(RecipeIngredient::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList()
                : Collections.emptyList();
        categoryCombo.setItems(existingCategories);
        categoryCombo.addCustomValueSetListener(e -> categoryCombo.setValue(e.getDetail()));

        TextField originalTextField = new TextField("Original Text");
        originalTextField.setWidthFull();
        originalTextField.setPlaceholder("e.g., 2 szklanki mąki pszennej");

        layout.add(ingredientCombo, quantityField, unitCombo, categoryCombo, originalTextField);

        BervanButton saveBtn = new BervanButton("Add");
        saveBtn.addClickListener(e -> {
            if (ingredientCombo.getValue() == null) {
                Notification.show("Select an ingredient");
                return;
            }

            RecipeIngredient ri = new RecipeIngredient();
            ri.setId(UUID.randomUUID());
            ri.setRecipe(recipe);
            ri.setIngredient(ingredientCombo.getValue());
            ri.setQuantity(quantityField.getValue());
            ri.setUnit(unitCombo.getValue());
            ri.setCategory(categoryCombo.getValue());
            ri.setOriginalText(originalTextField.getValue());
            ri.setOptional(false);
            ri.getOwners().addAll(recipe.getOwners());

            recipe.getRecipeIngredients().add(ri);
            recipeService.save(recipe);
            dialog.close();
            init(recipe.getId().toString());
        });

        dialog.add(layout);
        dialog.getFooter().add(saveBtn);
        dialog.open();
    }

    private Component buildInstructionSection(Recipe recipe) {
        Div section = new Div();
        section.addClassName("bervan-glass-card");
        section.addClassName("pm-section");
        section.getStyle().set("width", "100%");

        H3 title = new H3("Instructions");
        section.add(title);

        WysiwygTextArea wysiwygTextArea = new WysiwygTextArea("Instructions");
        wysiwygTextArea.setWidthFull();
        if (recipe.getInstruction() != null) {
            wysiwygTextArea.setValue(recipe.getInstruction());
        }

        BervanButton saveInstructionBtn = new BervanButton("Save Instructions");
        saveInstructionBtn.addClickListener(e -> {
            recipe.setInstruction(wysiwygTextArea.getValue());
            recipeService.save(recipe);
            showSuccessNotification("Instructions saved!");
        });

        section.add(wysiwygTextArea, saveInstructionBtn);
        return section;
    }

    private Component buildAddToCartSection(Recipe recipe) {
        Div section = new Div();
        section.addClassName("bervan-glass-card");
        section.addClassName("pm-section");
        section.getStyle().set("width", "100%");

        H3 title = new H3("Add to Shopping Cart");
        section.add(title);

        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.END);

        ComboBox<ShoppingCart> cartCombo = new ComboBox<>("Select Cart");
        cartCombo.setItemLabelGenerator(ShoppingCart::getName);
        cartCombo.setItems(query -> {
            int offset = query.getOffset();
            int limit = query.getLimit();
            List<ShoppingCart> carts = shoppingCartService.load(
                    new com.bervan.common.search.SearchRequest(),
                    org.springframework.data.domain.Pageable.ofSize(limit)
            ).stream().filter(c -> !Boolean.TRUE.equals(c.getArchived())).toList();
            int end = Math.min(offset + limit, carts.size());
            if (offset >= carts.size()) return java.util.stream.Stream.empty();
            return carts.subList(offset, end).stream();
        });

        NumberField servingsField = new NumberField("Servings multiplier");
        servingsField.setValue(1.0);
        servingsField.setMin(0.1);
        servingsField.setStep(0.5);

        BervanButton addToCartBtn = new BervanButton("Add to Cart");
        addToCartBtn.addClickListener(e -> {
            if (cartCombo.getValue() == null) {
                showErrorNotification("Select a cart");
                return;
            }
            double multiplier = servingsField.getValue() != null ? servingsField.getValue() : 1.0;
            shoppingCartService.addFromRecipe(cartCombo.getValue().getId(), recipe, multiplier);
            showSuccessNotification("Added to cart!");
        });

        row.add(cartCombo, servingsField, addToCartBtn);
        section.add(row);
        return section;
    }
}
