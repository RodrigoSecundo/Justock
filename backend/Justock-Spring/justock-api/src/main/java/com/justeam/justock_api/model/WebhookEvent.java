package com.justeam.justock_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_webhook")
@Data
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "usuario_marketplace_id", nullable = true)
    private Integer usuarioMarketplaceId;

    @Column(name = "marketplace_id", nullable = true)
    private Integer marketplaceId;

    @Column(name = "tipo_evento", nullable = true)
    private String tipoEvento;

    @Column(name = "payload", nullable = true, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "received_at", nullable = true)
    private LocalDateTime receivedAt;

    @Column(name = "processed", nullable = true)
    private Boolean processed;

    @Column(name = "processed_at", nullable = true)
    private LocalDateTime processedAt;

    @Column(name = "error", nullable = true)
    private String error;
}
