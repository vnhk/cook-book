package com.bervan.cookbook.view;

import com.bervan.common.component.BervanButton;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.cookbook.model.Recipe;
import com.bervan.cookbook.service.RecipeImportService;
import com.bervan.cookbook.service.RecipeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.UUID;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractRecipeListView extends AbstractBervanTableView<UUID, Recipe> {
    public static final String ROUTE_NAME = "/cook-book/recipes";

    private final RecipeImportService recipeImportService;

    public AbstractRecipeListView(RecipeService service, RecipeImportService recipeImportService,
                                   BervanViewConfig bervanViewConfig) {
        super(new CookBookPageLayout(ROUTE_NAME, AbstractRecipeDetailView.ROUTE_NAME),
                service, bervanViewConfig, Recipe.class);
        this.recipeImportService = recipeImportService;
        renderCommonComponents();
    }

    @Override
    protected Grid<Recipe> getGrid() {
        Grid<Recipe> grid = new Grid<>(Recipe.class, false);
        buildGridAutomatically(grid);

        if (grid.getColumnByKey("name") != null) {
            grid.getColumnByKey("name").setRenderer(new ComponentRenderer<>(
                    recipe -> new Anchor(AbstractRecipeDetailView.ROUTE_NAME + recipe.getId(), recipe.getName())
            ));
        }

        if (grid.getColumnByKey("favorite") != null) {
            grid.getColumnByKey("favorite").setRenderer(new ComponentRenderer<>(
                    recipe -> CookBookUIHelper.createFavoriteIcon(recipe.getFavorite())
            )).setHeader("").setWidth("50px").setFlexGrow(0);
        }

        if (grid.getColumnByKey("averageRating") != null) {
            grid.getColumnByKey("averageRating").setRenderer(new ComponentRenderer<>(
                    recipe -> CookBookUIHelper.createRatingStars(recipe.getAverageRating())
            )).setHeader("Rating").setWidth("150px");
        }

        if (grid.getColumnByKey("totalTime") != null) {
            grid.getColumnByKey("totalTime").setRenderer(new ComponentRenderer<>(
                    recipe -> CookBookUIHelper.createTimeBadge(recipe.getTotalTime())
            )).setHeader("Total Time");
        }

        return grid;
    }

    @Override
    protected void customizeTopTableActions(HorizontalLayout topTableActions) {
        super.customizeTopTableActions(topTableActions);

        Button importBtn = new BervanButton(new Icon(VaadinIcon.DOWNLOAD), e -> openImportDialog());
        importBtn.addClassName("bervan-icon-btn");
        importBtn.addClassName("accent");
        importBtn.getElement().setAttribute("title", "Import from URL");
        topTableActions.addComponentAtIndex(1, importBtn);
    }

    private void openImportDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setCloseOnOutsideClick(false);

        H3 title = new H3("Import Recipe from URL");
        title.getStyle().set("margin", "0");

        Button closeButton = new BervanButton(new Icon(VaadinIcon.CLOSE), e -> dialog.close());
        closeButton.addClassName("bervan-icon-btn");

        HorizontalLayout header = new HorizontalLayout(title, closeButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        TextField urlField = new TextField("Recipe URL");
        urlField.setWidthFull();
        urlField.setPlaceholder("https://...");

        Button importButton = new BervanButton("Import", e -> {
            String url = urlField.getValue();
            if (url == null || url.isBlank()) {
                showErrorNotification("Enter a URL");
                return;
            }
            try {
                Recipe imported = recipeImportService.importFromScraped(
                        recipeImportService.scrapePreview(url));
                showSuccessNotification("Recipe imported: " + imported.getName());
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                showErrorNotification("Import failed: " + ex.getMessage());
            }
        });
        importButton.addClassName("bervan-icon-btn");
        importButton.addClassName("primary");

        VerticalLayout content = new VerticalLayout(header, urlField, importButton);
        content.setSpacing(true);
        content.setPadding(true);
        content.setAlignItems(FlexComponent.Alignment.STRETCH);

        dialog.add(content);
        dialog.open();
    }
}
