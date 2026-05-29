package com.justeam.justock_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(
        name = "marketplace_listing",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_marketplace_listing_usuario_source_resource",
                columnNames = {"usuario", "marketplace_source", "marketplace_resource_id"}))
@Data
public class MarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "usuario", nullable = false)
    private Integer usuario;

    @Column(name = "marketplace_source", nullable = false)
    private String marketplaceSource;

    @Column(name = "marketplace_resource_id", nullable = false)
    private String marketplaceResourceId;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "marca")
    private String marca;

    @Column(name = "codigo_de_barras")
    private String codigoDeBarras;

    @Column(name = "preco")
    private BigDecimal preco;

    @Column(name = "quantidade_disponivel")
    private Integer quantidadeDisponivel;

    @Column(name = "produto_vinculado_id")
    private Integer produtoVinculadoId;

    @Column(name = "separado_manutencao", nullable = false)
    private Boolean separadoManualmente;
}