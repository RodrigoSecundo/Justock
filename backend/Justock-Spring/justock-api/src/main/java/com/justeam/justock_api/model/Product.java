package com.justeam.justock_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "estoque")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Integer idProduto;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Column(name = "marca", nullable = false)
    private String marca;

    @Column(name = "nome_do_produto", nullable = false)
    private String nomeDoProduto;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "preco", nullable = false)
    private BigDecimal preco;

    @Column(name = "codigo_de_barras", nullable = false)
    private String codigoDeBarras;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "quantidade_reservada", nullable = false)
    private Integer quantidadeReservada;

    @Column(name = "marcador", nullable = false)
    private String marcador;

    @Column(name = "usuario", nullable = false)
    private Integer usuario;
}
