package com.justeam.justock_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserMarketplaceResponseDTO {
    private int usuarioMarketplaceId;
    private int usuario;
    private int marketplaceId;
    private String idLoja;
    private String nomeLoja;
    private String clienteId;
    private String clienteSecret;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiration;
    private String statusIntegracao;

    public UserMarketplaceResponseDTO(int usuarioMarketplaceId, int usuario, int marketplaceId, String idLoja, String nomeLoja, String clienteId, String clienteSecret, String accessToken, String refreshToken, LocalDateTime tokenExpiration, String statusIntegracao) {
        this.usuarioMarketplaceId = usuarioMarketplaceId;
        this.usuario = usuario;
        this.marketplaceId = marketplaceId;
        this.idLoja = idLoja;
        this.nomeLoja = nomeLoja;
        this.clienteId = clienteId;
        this.clienteSecret = clienteSecret;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiration = tokenExpiration;
        this.statusIntegracao = statusIntegracao;
    }
}
