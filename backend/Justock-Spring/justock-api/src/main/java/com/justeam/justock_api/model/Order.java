package com.justeam.justock_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "pedido")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private int idPedido;

    @Column(name = "id_pedido_marketplace", nullable = true)
    private String idPedidoMarketplace;

    @Column(name = "usuario_marketplace_id", nullable = true)
    private Integer usuarioMarketplaceId;

    @Column(name = "data_entrega", nullable = true)
    private LocalDate dataEntrega;

    @Column(name = "data_emissao", nullable = true)
    private LocalDate dataEmissao;

    @Column(name = "status_pagamento", nullable = true)
    private String statusPagamento;

    @Column(name = "status_pedido", nullable = true)
    private String statusPedido;
}
