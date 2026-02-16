package com.bervan.cookbook.repository;

import com.bervan.cookbook.model.ShoppingCart;
import com.bervan.history.model.BaseRepository;

import java.util.UUID;

public interface ShoppingCartRepository extends BaseRepository<ShoppingCart, UUID> {
}
