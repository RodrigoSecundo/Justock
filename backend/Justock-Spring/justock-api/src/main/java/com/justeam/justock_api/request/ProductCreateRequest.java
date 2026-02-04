package com.justeam.justock_api.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductCreateRequest {

    @NotBlank(message = "O campo categoria é obrigatório.")
    private String categoria;

    @NotBlank(message = "O campo marca é obrigatório.")
    private String marca;

    @NotBlank(message = "O campo nomeDoProduto é obrigatório.")
    private String nomeDoProduto;

    @NotBlank(message = "O campo estado é obrigatório.")
    private String estado;

    @NotNull(message = "O campo preco é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preco deve ser maior que 0")
    private BigDecimal preco;

    @NotBlank(message = "O campo codigoDeBarras é obrigatório.")
    private String codigoDeBarras;

    @NotNull(message = "O campo quantidade é obrigatório.")
    @Min(value = 0, message = "A quantidade deve ser no mínimo 0")
    private Integer quantidade;

    @NotNull(message = "O campo quantidadeReservada é obrigatório.")
    @Min(value = 0, message = "A quantidadeReservada deve ser no mínimo 0")
    private Integer quantidadeReservada;

    @NotBlank(message = "O campo marcador é obrigatório.")
    private String marcador;

    @NotNull(message = "O campo usuario é obrigatório.")
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
