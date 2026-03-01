package com.bervan.cookbook.view;

import com.bervan.common.component.BervanButton;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.view.AbstractPageView;
import com.bervan.cookbook.model.CulinaryUnit;
import com.bervan.cookbook.model.ShoppingCart;
import com.bervan.cookbook.model.ShoppingCartItem;
import com.bervan.cookbook.service.ShoppingCartService;
import com.bervan.cookbook.service.UnitConversionEngine;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

@CssImport("./bervan-cookbook.css")
public abstract class AbstractShoppingCartView extends AbstractPageView {
    public static final String ROUTE_NAME = "/cook-book/shopping-cart";

    private final ShoppingCartService shoppingCartService;
    private final UnitConversionEngine unitConversionEngine;
    private final CookBookPageLayout pageLayout = new CookBookPageLayout(ROUTE_NAME);
    private ShoppingCart selectedCart;

    public AbstractShoppingCartView(ShoppingCartService shoppingCartService,
                                     UnitConversionEngine unitConversionEngine) {
        this.shoppingCartService = shoppingCartService;
        this.unitConversionEngine = unitConversionEngine;
        add(pageLayout);
        buildView();
    }

    private void buildView() {
        getChildren().filter(c -> c != pageLayout).toList().forEach(this::remove);
        setWidthFull();

        Div mainSection = new Div();
        mainSection.addClassName("bervan-glass-card");
        mainSection.addClassName("pm-section");
        mainSection.getStyle().set("width", "100%");

        // Cart selector
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setWidthFull();
        topBar.setAlignItems(FlexComponent.Alignment.END);

        ComboBox<ShoppingCart> cartCombo = new ComboBox<>("Shopping Cart");
        cartCombo.setItemLabelGenerator(ShoppingCart::getName);
        cartCombo.setItems(query -> {
            int offset = query.getOffset();
            int limit = query.getLimit();
            List<ShoppingCart> carts = new ArrayList<>(shoppingCartService.load(
                    new SearchRequest(), Pageable.ofSize(100)
            ));
            carts.removeIf(c -> Boolean.TRUE.equals(c.isDeleted()));
            int end = Math.min(offset + limit, carts.size());
            if (offset >= carts.size()) return java.util.stream.Stream.empty();
            return carts.subList(offset, end).stream();
        });
        cartCombo.addValueChangeListener(e -> {
            selectedCart = e.getValue();
            buildView();
        });

        BervanButton newCartBtn = new BervanButton("New Cart");
        newCartBtn.addClickListener(e -> openNewCartDialog());

        topBar.add(cartCombo, newCartBtn);
        mainSection.add(topBar);

        if (selectedCart != null) {
            // Reload fresh
            Optional<ShoppingCart> freshCart = shoppingCartService.loadById(selectedCart.getId());
            if (freshCart.isPresent()) {
                selectedCart = freshCart.get();
                mainSection.add(buildCartItemsGrid(selectedCart));
                mainSection.add(buildCartActions(selectedCart));
            }
        }

        add(mainSection);
    }

    private Component buildCartItemsGrid(ShoppingCart cart) {
        Grid<ShoppingCartItem> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        List<ShoppingCartItem> items = cart.getItems().stream()
                .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                .sorted(Comparator.comparing(i -> i.getIngredient().getName()))
                .collect(Collectors.toList());

        grid.setItems(items);

        grid.addColumn(new ComponentRenderer<>(item -> {
            Checkbox cb = new Checkbox();
            cb.setValue(Boolean.TRUE.equals(item.getPurchased()));
            cb.addValueChangeListener(e -> {
                shoppingCartService.togglePurchased(cart, item.getId());
                buildView();
            });
            return cb;
        })).setHeader("").setWidth("50px");

        grid.addColumn(item -> {
            if (item.getQuantity() != null && item.getUnit() != null) {
                return unitConversionEngine.formatQuantity(item.getQuantity(), item.getUnit());
            }
            return "-";
        }).setHeader("Quantity").setWidth("120px");

        grid.addColumn(item -> item.getIngredient().getName()).setHeader("Ingredient").setFlexGrow(1);

        grid.addColumn(item -> item.getSourceRecipe() != null ? item.getSourceRecipe().getName() : "")
                .setHeader("Source Recipe").setFlexGrow(1);

        grid.addColumn(new ComponentRenderer<>(item -> {
            Span name = new Span(item.getIngredient().getName());
            if (Boolean.TRUE.equals(item.getPurchased())) {
                name.addClassName("cb-purchased-item");
            }
            return name;
        })).setVisible(false); // Hidden column for styling reference

        return grid;
    }

    private Component buildCartActions(ShoppingCart cart) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.getStyle().set("margin-top", "var(--bervan-spacing-md)");

        BervanButton exportBtn = new BervanButton("Export to Text");
        exportBtn.addClickListener(e -> {
            String text = shoppingCartService.exportToText(cart.getId());
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Shopping List");
            dialog.setWidth("500px");
            Pre pre = new Pre(text);
            pre.getStyle().set("white-space", "pre-wrap");
            dialog.add(pre);
            dialog.open();
        });

        BervanButton archiveBtn = new BervanButton("Archive Cart");
        archiveBtn.addClickListener(e -> {
            cart.setArchived(true);
            shoppingCartService.save(cart);
            selectedCart = null;
            buildView();
            showSuccessNotification("Cart archived!");
        });

        BervanButton deleteBtn = new BervanButton("Delete Cart");
        deleteBtn.addClickListener(e -> {
            shoppingCartService.delete(cart);
            selectedCart = null;
            buildView();
            showSuccessNotification("Cart deleted!");
        });

        actions.add(exportBtn, archiveBtn, deleteBtn);
        return actions;
    }

    private void openNewCartDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New Shopping Cart");

        TextField nameField = new TextField("Cart Name");
        nameField.setWidthFull();

        BervanButton saveBtn = new BervanButton("Create");
        saveBtn.addClickListener(e -> {
            if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                showErrorNotification("Enter a name");
                return;
            }
            ShoppingCart cart = new ShoppingCart();
            cart.setId(UUID.randomUUID());
            cart.setName(nameField.getValue().trim());
            cart.setArchived(false);
            selectedCart = shoppingCartService.save(cart);
            dialog.close();
            buildView();
            showSuccessNotification("Cart created!");
        });

        dialog.add(nameField);
        dialog.getFooter().add(saveBtn);
        dialog.open();
    }
}
