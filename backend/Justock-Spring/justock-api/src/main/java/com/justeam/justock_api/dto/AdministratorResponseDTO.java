package com.justeam.justock_api.dto;

import lombok.Data;

@Data
public class AdministratorResponseDTO {
    private int idAdministrador;
    private String emailCorporativo;
    private String senha;

    public AdministratorResponseDTO(int idAdministrador, String emailCorporativo, String senha) {
        this.idAdministrador = idAdministrador;
        this.emailCorporativo = emailCorporativo;
        this.senha = senha;
    }
}
