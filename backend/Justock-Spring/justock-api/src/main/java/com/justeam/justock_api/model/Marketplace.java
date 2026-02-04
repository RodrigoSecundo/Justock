package com.justeam.justock_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace")
@Data
public class Marketplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "marketplace_id", nullable = false)
    private int marketplaceId;

    @Column(name = "nome_marketplace")
    private String nomeMarketplace;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
