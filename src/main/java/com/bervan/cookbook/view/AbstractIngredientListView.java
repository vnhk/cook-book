package com.bervan.cookbook.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.cookbook.model.Ingredient;
import com.bervan.cookbook.service.IngredientService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.UUID;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractIngredientListView extends AbstractBervanTableView<UUID, Ingredient> {
    public static final String ROUTE_NAME = "/cook-book/ingredients";

    public AbstractIngredientListView(IngredientService service, BervanViewConfig bervanViewConfig) {
        super(new CookBookPageLayout(ROUTE_NAME), service, bervanViewConfig, Ingredient.class);
        renderCommonComponents();
    }

    @Override
    protected Grid<Ingredient> getGrid() {
        Grid<Ingredient> grid = new Grid<>(Ingredient.class, false);
        buildGridAutomatically(grid);

        if (grid.getColumnByKey("category") != null) {
            grid.getColumnByKey("category").setRenderer(new ComponentRenderer<>(
                    ingredient -> CookBookUIHelper.createCategoryBadge(ingredient.getCategory())
            ));
        }

        return grid;
    }
}
