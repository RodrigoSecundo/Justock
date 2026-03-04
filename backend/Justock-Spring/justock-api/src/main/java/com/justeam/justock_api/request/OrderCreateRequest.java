package com.justeam.justock_api.request;

import java.time.LocalDate;

public class OrderCreateRequest {

    private Integer idPedidoMarketplace;
    private Integer usuarioMarketplaceId;
    private LocalDate dataEntrega;
    private LocalDate dataEmissao;
    private String statusPagamento;
    private String statusPedido;

    // Getters e Setters
    public Integer getIdPedidoMarketplace() { return idPedidoMarketplace; }
    public void setIdPedidoMarketplace(Integer idPedidoMarketplace) { this.idPedidoMarketplace = idPedidoMarketplace; }

    public Integer getUsuarioMarketplaceId() { return usuarioMarketplaceId; }
    public void setUsuarioMarketplaceId(Integer usuarioMarketplaceId) { this.usuarioMarketplaceId = usuarioMarketplaceId; }

    public LocalDate getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDate dataEntrega) { this.dataEntrega = dataEntrega; }

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }

    public String getStatusPagamento() { return statusPagamento; }
    public void setStatusPagamento(String statusPagamento) { this.statusPagamento = statusPagamento; }

    public String getStatusPedido() { return statusPedido; }
    public void setStatusPedido(String statusPedido) { this.statusPedido = statusPedido; }
}
