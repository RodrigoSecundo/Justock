package com.justeam.justock_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class OrderItemId implements Serializable {

    @Column(name = "id_produto", nullable = false)
    private Integer idProduto;

    @Column(name = "id_pedido", nullable = false)
    private Integer idPedido;
}