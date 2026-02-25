package com.justeam.justock_api.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private int idUsuario;
    private String nomeUsuario;
    private String emailCorporativo;
    private String numero;
    private String senha;

    public UserResponseDTO(int idUsuario, String nomeUsuario, String emailCorporativo, String numero, String senha) {
        this.idUsuario = idUsuario;
        this.nomeUsuario = nomeUsuario;
        this.emailCorporativo = emailCorporativo;
        this.numero = numero;
        this.senha = senha;
    }
}
