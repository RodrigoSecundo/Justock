package com.justeam.justock_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "estoque_pedido")
@Data
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    @Column(name = "quantidade")
    private Integer quantidade;

    @Column(name = "preco_unitario")
    private BigDecimal precoUnitario;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "id_item_marketplace")
    private String idItemMarketplace;

    @Column(name = "item_status")
    private String itemStatus;
}