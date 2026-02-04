package com.justeam.justock_api.dto;

import lombok.Data;

@Data
public class StockOrderResponseDTO {
    private int idProduto;
    private int idPedido;
    private Integer quantidade;
    private Double precoUnitario;
    private Double subtotal;
    private String idItemMarketplace;
    private String itemStatus;

    public StockOrderResponseDTO(int idProduto, int idPedido, Integer quantidade, Double precoUnitario, Double subtotal, String idItemMarketplace, String itemStatus) {
        this.idProduto = idProduto;
        this.idPedido = idPedido;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = subtotal;
        this.idItemMarketplace = idItemMarketplace;
        this.itemStatus = itemStatus;
    }
}
