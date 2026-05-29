package com.justeam.justock_api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    private String observacao;
    private Boolean inventoryApplied;
    private BigDecimal valorTotal;
    private List<OrderItemResponseDTO> itens;

    public OrderResponseDTO(int idPedido, Integer idPedidoMarketplace, Integer usuarioMarketplaceId, LocalDate dataEntrega, LocalDate dataEmissao, String statusPagamento, String statusPedido, String marketplaceResourceId, String marketplaceSource, String observacao, Boolean inventoryApplied, BigDecimal valorTotal, List<OrderItemResponseDTO> itens) {
        this.idPedido = idPedido;
        this.idPedidoMarketplace = idPedidoMarketplace;
        this.usuarioMarketplaceId = usuarioMarketplaceId;
        this.dataEntrega = dataEntrega;
        this.dataEmissao = dataEmissao;
        this.statusPagamento = statusPagamento;
        this.statusPedido = statusPedido;
        this.marketplaceResourceId = marketplaceResourceId;
        this.marketplaceSource = marketplaceSource;
        this.observacao = observacao;
        this.inventoryApplied = inventoryApplied;
        this.valorTotal = valorTotal;
        this.itens = itens;
    }
}
