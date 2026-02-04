package com.justeam.justock_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_marketplace")
@Data
public class UserMarketplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_marketplace_id")
    private int usuarioMarketplaceId;

    @Column(nullable = false)
    private int usuario;

    @Column(nullable = false)
    private int marketplaceId;

    @Column(nullable = false, length = 255)
    private String idLoja;

    @Column(nullable = false, length = 255)
    private String nomeLoja;

    @Column(nullable = false, length = 255)
    private String clienteId;

    @Column(nullable = false, length = 255)
    private String clienteSecret;

    @Column(nullable = false, length = 255)
    private String accessToken;

    @Column(nullable = false, length = 255)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime tokenExpiration;

    @Column(nullable = false, length = 255)
    private String statusIntegracao;
}
