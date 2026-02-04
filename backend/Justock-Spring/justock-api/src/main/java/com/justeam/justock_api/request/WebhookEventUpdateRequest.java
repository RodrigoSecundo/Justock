package com.justeam.justock_api.request;

import java.time.LocalDateTime;

public class WebhookEventUpdateRequest {

    private Integer usuarioMarketplaceId;

    private Integer marketplaceId;

    private String tipoEvento;

    private String payload;

    private LocalDateTime receivedAt;

    private Boolean processed;

    private LocalDateTime processedAt;

    private String error;

    // Getters e Setters
    public Integer getUsuarioMarketplaceId() { return usuarioMarketplaceId; }
    public void setUsuarioMarketplaceId(Integer usuarioMarketplaceId) { this.usuarioMarketplaceId = usuarioMarketplaceId; }

    public Integer getMarketplaceId() { return marketplaceId; }
    public void setMarketplaceId(Integer marketplaceId) { this.marketplaceId = marketplaceId; }

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
