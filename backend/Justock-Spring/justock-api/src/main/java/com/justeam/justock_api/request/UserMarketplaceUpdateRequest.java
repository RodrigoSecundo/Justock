package com.justeam.justock_api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class UserMarketplaceUpdateRequest {

    @NotNull(message = "O campo usuario é obrigatório.")
    private int usuario;

    @NotNull(message = "O campo marketplaceId é obrigatório.")
    private int marketplaceId;

    private String idLoja;

    private String nomeLoja;

    private String clienteId;

    private String clienteSecret;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime tokenExpiration;

    private String statusIntegracao;

    // Getters e Setters
    public int getUsuario() { return usuario; }
    public void setUsuario(int usuario) { this.usuario = usuario; }

    public int getMarketplaceId() { return marketplaceId; }
    public void setMarketplaceId(int marketplaceId) { this.marketplaceId = marketplaceId; }

    public String getIdLoja() { return idLoja; }
    public void setIdLoja(String idLoja) { this.idLoja = idLoja; }

    public String getNomeLoja() { return nomeLoja; }
    public void setNomeLoja(String nomeLoja) { this.nomeLoja = nomeLoja; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getClienteSecret() { return clienteSecret; }
    public void setClienteSecret(String clienteSecret) { this.clienteSecret = clienteSecret; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getTokenExpiration() { return tokenExpiration; }
    public void setTokenExpiration(LocalDateTime tokenExpiration) { this.tokenExpiration = tokenExpiration; }

    public String getStatusIntegracao() { return statusIntegracao; }
    public void setStatusIntegracao(String statusIntegracao) { this.statusIntegracao = statusIntegracao; }
}
