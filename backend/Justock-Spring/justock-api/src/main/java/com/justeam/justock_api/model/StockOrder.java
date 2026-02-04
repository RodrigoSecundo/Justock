package com.justeam.justock_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estoque_pedido")
@Data
public class StockOrder {

    @Id
    @Column(name = "id_produto", nullable = false)
    private int idProduto;

    @Column(name = "id_pedido", nullable = false)
    private int idPedido;

    @Column(name = "quantidade")
    private Integer quantidade;

    @Column(name = "preco_unitario")
    private Double precoUnitario;

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "id_item_marketplace")
    private String idItemMarketplace;

    @Column(name = "item_status")
    private String itemStatus;
}
