package com.justeam.justock_api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class UserMarketplaceCreateRequest {

    @NotNull(message = "O campo usuario é obrigatório.")
    private int usuario;

    @NotNull(message = "O campo marketplaceId é obrigatório.")
    private int marketplaceId;

    @NotBlank(message = "O campo idLoja é obrigatório.")
    private String idLoja;

    @NotBlank(message = "O campo nomeLoja é obrigatório.")
    private String nomeLoja;

    @NotBlank(message = "O campo clienteId é obrigatório.")
    private String clienteId;

    @NotBlank(message = "O campo clienteSecret é obrigatório.")
    private String clienteSecret;

    @NotBlank(message = "O campo accessToken é obrigatório.")
    private String accessToken;

    @NotBlank(message = "O campo refreshToken é obrigatório.")
    private String refreshToken;

    @NotNull(message = "O campo tokenExpiration é obrigatório.")
    private LocalDateTime tokenExpiration;

    @NotBlank(message = "O campo statusIntegracao é obrigatório.")
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
