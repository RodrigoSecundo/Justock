package com.justeam.justock_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WebhookEventResponseDTO {
    private int id;
    private Integer usuarioMarketplaceId;
    private Integer marketplaceId;
    private String tipoEvento;
    private String payload;
    private LocalDateTime receivedAt;
    private Boolean processed;
    private LocalDateTime processedAt;
    private String error;

    public WebhookEventResponseDTO(int id, Integer usuarioMarketplaceId, Integer marketplaceId, String tipoEvento, String payload, LocalDateTime receivedAt, Boolean processed, LocalDateTime processedAt, String error) {
        this.id = id;
        this.usuarioMarketplaceId = usuarioMarketplaceId;
        this.marketplaceId = marketplaceId;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.processed = processed;
        this.processedAt = processedAt;
        this.error = error;
    }
}
