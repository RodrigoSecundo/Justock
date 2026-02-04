package com.justeam.justock_api.request;

public class StockOrderUpdateRequest {

    private int idPedido;
    private Integer quantidade;
    private Double precoUnitario;
    private Double subtotal;
    private String idItemMarketplace;
    private String itemStatus;

    // Getters e Setters
    public int getIdPedido() { return idPedido; }
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(Double precoUnitario) { this.precoUnitario = precoUnitario; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public String getIdItemMarketplace() { return idItemMarketplace; }
    public void setIdItemMarketplace(String idItemMarketplace) { this.idItemMarketplace = idItemMarketplace; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }
}
