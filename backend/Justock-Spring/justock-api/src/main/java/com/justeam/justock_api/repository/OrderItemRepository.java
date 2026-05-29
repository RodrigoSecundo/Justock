package com.justeam.justock_api.repository;

import com.justeam.justock_api.model.OrderItem;
import com.justeam.justock_api.model.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
    List<OrderItem> findByIdIdPedido(Integer idPedido);

    void deleteByIdIdPedido(Integer idPedido);

    boolean existsByIdIdProduto(Integer idProduto);
}