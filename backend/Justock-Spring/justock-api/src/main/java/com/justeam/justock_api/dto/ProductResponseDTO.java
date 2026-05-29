package com.justeam.justock_api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductResponseDTO {
    private Integer idProduto;
    private String categoria;
    private String marca;
    private String nomeDoProduto;
    private String estado;
    private BigDecimal preco;
    private String codigoDeBarras;
    private Integer quantidade;
    private Integer quantidadeReservada;
    private String marcador;
    private Integer usuario;
    private String marketplaceResourceId;
    private String marketplaceSource;
    private String tipoRegistro;
    private String statusVinculo;
    private Integer produtoVinculadoId;
    private String produtoVinculadoNome;
    private Integer inventoryProductId;
    private Integer quantidadeAnunciosVinculados;
    private Boolean permiteEscolherProduto;
    private Boolean permiteManterSeparado;

    public ProductResponseDTO(Integer idProduto, String categoria, String marca, String nomeDoProduto, String estado, BigDecimal preco, String codigoDeBarras, Integer quantidade, Integer quantidadeReservada, String marcador, Integer usuario, String marketplaceResourceId, String marketplaceSource) {
        this.idProduto = idProduto;
        this.categoria = categoria;
        this.marca = marca;
        this.nomeDoProduto = nomeDoProduto;
        this.estado = estado;
        this.preco = preco;
        this.codigoDeBarras = codigoDeBarras;
        this.quantidade = quantidade;
        this.quantidadeReservada = quantidadeReservada;
        this.marcador = marcador;
        this.usuario = usuario;
        this.marketplaceResourceId = marketplaceResourceId;
        this.marketplaceSource = marketplaceSource;
        this.tipoRegistro = "PRODUTO";
        this.statusVinculo = "NAO_VINCULADO";
        this.produtoVinculadoId = idProduto;
        this.produtoVinculadoNome = nomeDoProduto;
        this.inventoryProductId = idProduto;
        this.quantidadeAnunciosVinculados = 0;
        this.permiteEscolherProduto = Boolean.FALSE;
        this.permiteManterSeparado = Boolean.FALSE;
    }
}
