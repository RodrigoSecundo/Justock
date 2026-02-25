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

    public ProductResponseDTO(Integer idProduto, String categoria, String marca, String nomeDoProduto, String estado, BigDecimal preco, String codigoDeBarras, Integer quantidade, Integer quantidadeReservada, String marcador, Integer usuario) {
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
    }
}
