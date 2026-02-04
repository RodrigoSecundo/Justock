package com.justeam.justock_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MarketplaceResponseDTO {
    private int marketplaceId;
    private String nomeMarketplace;
    private String apiUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MarketplaceResponseDTO(int marketplaceId, String nomeMarketplace, String apiUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.marketplaceId = marketplaceId;
        this.nomeMarketplace = nomeMarketplace;
        this.apiUrl = apiUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
