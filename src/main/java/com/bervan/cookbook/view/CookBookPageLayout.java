package com.bervan.cookbook.view;

import com.bervan.common.MenuNavigationComponent;
import com.vaadin.flow.component.icon.VaadinIcon;

public class CookBookPageLayout extends MenuNavigationComponent {
    public CookBookPageLayout(String route, String... notVisibleButtons) {
        super(route, notVisibleButtons);

        addButtonIfVisible(menuButtonsRow, AbstractRecipeListView.ROUTE_NAME, "Recipes", VaadinIcon.BOOK.create());
        addButtonIfVisible(menuButtonsRow, AbstractIngredientListView.ROUTE_NAME, "Ingredients", VaadinIcon.LIST.create());
        addButtonIfVisible(menuButtonsRow, AbstractShoppingCartView.ROUTE_NAME, "Cart", VaadinIcon.CART.create());
        addButtonIfVisible(menuButtonsRow, AbstractRecipeSearchView.ROUTE_NAME, "Search", VaadinIcon.SEARCH.create());
        addButtonIfVisible(menuButtonsRow, AbstractRecipeDetailView.ROUTE_NAME, "Recipe", VaadinIcon.FILE_TEXT.create());

        add(menuButtonsRow);
    }
}
