package com.justeam.justock_api.dto;

import lombok.Data;

@Data
public class AdministratorResponseDTO {
    private int idAdministrador;
    private String emailCorporativo;

    public AdministratorResponseDTO(int idAdministrador, String emailCorporativo) {
        this.idAdministrador = idAdministrador;
        this.emailCorporativo = emailCorporativo;
    }
}
