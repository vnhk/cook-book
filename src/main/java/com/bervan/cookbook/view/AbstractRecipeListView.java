package com.bervan.cookbook.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.cookbook.model.Recipe;
import com.bervan.cookbook.service.RecipeService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.UUID;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractRecipeListView extends AbstractBervanTableView<UUID, Recipe> {
    public static final String ROUTE_NAME = "/cook-book/recipes";

    public AbstractRecipeListView(RecipeService service, BervanViewConfig bervanViewConfig) {
        super(new CookBookPageLayout(ROUTE_NAME, AbstractRecipeDetailView.ROUTE_NAME),
                service, bervanViewConfig, Recipe.class);
        renderCommonComponents();
    }

    @Override
    protected Grid<Recipe> getGrid() {
        Grid<Recipe> grid = new Grid<>(Recipe.class, false);
        buildGridAutomatically(grid);

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
    protected void preColumnAutoCreation(Grid<Recipe> grid) {
        grid.addComponentColumn(entity -> {
            Icon linkIcon = new Icon(VaadinIcon.LINK);
            linkIcon.getStyle().set("cursor", "pointer");
            return new Anchor(AbstractRecipeDetailView.ROUTE_NAME + entity.getId(),
                    new HorizontalLayout(linkIcon));
        }).setKey("link").setWidth("6px").setResizable(false);
    }
}
