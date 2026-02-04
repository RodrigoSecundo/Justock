package com.justeam.justock_api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MarketplaceCreateRequest {

    @NotBlank(message = "O campo nome_marketplace é obrigatório.")
    @Size(max = 255, message = "O campo nome_marketplace deve ter no máximo {max} caracteres.")
    private String nomeMarketplace;

    @NotBlank(message = "O campo api_url é obrigatório.")
    @Size(max = 255, message = "O campo api_url deve ter no máximo {max} caracteres.")
    private String apiUrl;

    // Getters e Setters
    public String getNomeMarketplace() { return nomeMarketplace; }
    public void setNomeMarketplace(String nomeMarketplace) { this.nomeMarketplace = nomeMarketplace; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

}
