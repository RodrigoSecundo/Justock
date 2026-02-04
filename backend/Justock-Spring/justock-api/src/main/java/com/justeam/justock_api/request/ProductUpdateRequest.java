package com.justeam.justock_api.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public class ProductUpdateRequest {

    private String categoria;

    private String marca;

    private String nomeDoProduto;

    private String estado;

    @DecimalMin(value = "0.0", inclusive = false, message = "O preco deve ser maior que 0")
    private BigDecimal preco;

    private String codigoDeBarras;

    @Min(value = 0, message = "A quantidade deve ser no mínimo 0")
    private Integer quantidade;

    @Min(value = 0, message = "A quantidadeReservada deve ser no mínimo 0")
    private Integer quantidadeReservada;

    private String marcador;

    private Integer usuario;

    // Getters and Setters
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getNomeDoProduto() { return nomeDoProduto; }
    public void setNomeDoProduto(String nomeDoProduto) { this.nomeDoProduto = nomeDoProduto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public String getCodigoDeBarras() { return codigoDeBarras; }
    public void setCodigoDeBarras(String codigoDeBarras) { this.codigoDeBarras = codigoDeBarras; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Integer getQuantidadeReservada() { return quantidadeReservada; }
    public void setQuantidadeReservada(Integer quantidadeReservada) { this.quantidadeReservada = quantidadeReservada; }

    public String getMarcador() { return marcador; }
    public void setMarcador(String marcador) { this.marcador = marcador; }

    public Integer getUsuario() { return usuario; }
    public void setUsuario(Integer usuario) { this.usuario = usuario; }
}
