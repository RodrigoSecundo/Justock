package com.justeam.justock_api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponseDTO {
    private Integer idProduto;
    private String nomeDoProduto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
    private String idItemMarketplace;
    private String itemStatus;

    public OrderItemResponseDTO(Integer idProduto, String nomeDoProduto, Integer quantidade, BigDecimal precoUnitario, BigDecimal subtotal, String idItemMarketplace, String itemStatus) {
        this.idProduto = idProduto;
        this.nomeDoProduto = nomeDoProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = subtotal;
        this.idItemMarketplace = idItemMarketplace;
        this.itemStatus = itemStatus;
    }
}