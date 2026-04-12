package com.justeam.justock_api.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OrderResponseDTO {
    private int idPedido;
    private Integer idPedidoMarketplace;
    private Integer usuarioMarketplaceId;
    private LocalDate dataEntrega;
    private LocalDate dataEmissao;
    private String statusPagamento;
    private String statusPedido;
    private String marketplaceResourceId;
    private String marketplaceSource;

    public OrderResponseDTO(int idPedido, Integer idPedidoMarketplace, Integer usuarioMarketplaceId, LocalDate dataEntrega, LocalDate dataEmissao, String statusPagamento, String statusPedido, String marketplaceResourceId, String marketplaceSource) {
        this.idPedido = idPedido;
        this.idPedidoMarketplace = idPedidoMarketplace;
        this.usuarioMarketplaceId = usuarioMarketplaceId;
        this.dataEntrega = dataEntrega;
        this.dataEmissao = dataEmissao;
        this.statusPagamento = statusPagamento;
        this.statusPedido = statusPedido;
        this.marketplaceResourceId = marketplaceResourceId;
        this.marketplaceSource = marketplaceSource;
    }
}
