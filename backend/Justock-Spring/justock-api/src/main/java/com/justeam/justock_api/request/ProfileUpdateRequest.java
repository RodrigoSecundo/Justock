package com.justeam.justock_api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {

    @Size(max = 50, message = "O campo nome deve ter no máximo {max} caracteres.")
    private String name;

    private String numero;

    @Size(min = 8, message = "O campo senha deve ter no mínimo {min} caracteres.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).*$",
        message = "A senha deve conter ao menos uma letra maiúscula, uma letra minúscula, um número e um símbolo."
    )
    private String password;

    private String passwordConfirmation;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String passwordConfirmation) { this.passwordConfirmation = passwordConfirmation; }
}