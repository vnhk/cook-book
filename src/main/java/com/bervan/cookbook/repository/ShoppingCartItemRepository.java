package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.ShoppingCartItem;
import com.bervan.history.model.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface ShoppingCartItemRepository extends BaseRepository<ShoppingCartItem, UUID> {
    List<ShoppingCartItem> findByCartId(UUID cartId);
}
