package com.bervan.cookbook.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.cookbook.model.*;
import com.bervan.cookbook.repository.ShoppingCartItemRepository;
import com.bervan.cookbook.repository.ShoppingCartRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShoppingCartService extends BaseService<UUID, ShoppingCart> {
    private final ShoppingCartItemRepository itemRepository;
    private final UnitConversionEngine conversionEngine;

    public ShoppingCartService(ShoppingCartRepository repository, SearchService searchService,
                               ShoppingCartItemRepository itemRepository,
                               UnitConversionEngine conversionEngine) {
        super(repository, searchService);
        this.itemRepository = itemRepository;
        this.conversionEngine = conversionEngine;
    }

    public void addFromRecipe(UUID cartId, Recipe recipe, double servingMultiplier) {
        Optional<ShoppingCart> cartOpt = loadById(cartId);
        if (cartOpt.isEmpty() || recipe.getRecipeIngredients() == null) {
            return;
        }

        ShoppingCart cart = cartOpt.get();

        for (RecipeIngredient ri : recipe.getRecipeIngredients()) {
            if (Boolean.TRUE.equals(ri.isDeleted())) continue;

            double qty = ri.getQuantity() != null ? ri.getQuantity() * servingMultiplier : 0;
            CulinaryUnit unit = ri.getUnit() != null ? ri.getUnit() : CulinaryUnit.PIECE;

            // Try to aggregate with existing item
            Optional<ShoppingCartItem> existing = cart.getItems().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.isDeleted()))
                    .filter(item -> item.getIngredient().getId().equals(ri.getIngredient().getId()))
                    .filter(item -> item.getUnit() != null && conversionEngine.areCompatible(item.getUnit(), unit))
                    .findFirst();

            if (existing.isPresent()) {
                ShoppingCartItem item = existing.get();
                Double converted = conversionEngine.convert(qty, unit, item.getUnit());
                if (converted != null) {
                    item.setQuantity((item.getQuantity() != null ? item.getQuantity() : 0) + converted);
                }
            } else {
                ShoppingCartItem newItem = new ShoppingCartItem();
                newItem.setId(UUID.randomUUID());
                newItem.setCart(cart);
                newItem.setIngredient(ri.getIngredient());
                newItem.setQuantity(qty);
                newItem.setUnit(unit);
                newItem.setPurchased(false);
                newItem.setSourceRecipe(recipe);
                newItem.getOwners().addAll(cart.getOwners());
                cart.getItems().add(newItem);
            }
        }

        save(cart);
    }

    public void togglePurchased(ShoppingCart cart, UUID itemId) {
        cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> {
                    item.setPurchased(!Boolean.TRUE.equals(item.getPurchased()));
                });
        save(cart);
    }

    public String exportToText(UUID cartId) {
        Optional<ShoppingCart> cartOpt = loadById(cartId);
        if (cartOpt.isEmpty()) {
            return "";
        }

        ShoppingCart cart = cartOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append(cart.getName()).append("\n");
        sb.append("=".repeat(cart.getName().length())).append("\n\n");

        List<ShoppingCartItem> activeItems = cart.getItems().stream()
                .filter(item -> !Boolean.TRUE.equals(item.isDeleted()))
                .sorted(Comparator.comparing(item -> item.getIngredient().getName()))
                .collect(Collectors.toList());

        for (ShoppingCartItem item : activeItems) {
            String check = Boolean.TRUE.equals(item.getPurchased()) ? "[x] " : "[ ] ";
            String qtyStr = "";
            if (item.getQuantity() != null && item.getUnit() != null) {
                qtyStr = conversionEngine.formatQuantity(item.getQuantity(), item.getUnit()) + " ";
            }
            sb.append(check).append(qtyStr).append(item.getIngredient().getName()).append("\n");
        }

        return sb.toString();
    }
}
