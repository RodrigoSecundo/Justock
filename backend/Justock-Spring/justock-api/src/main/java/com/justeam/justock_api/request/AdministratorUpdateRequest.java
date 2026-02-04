package com.justeam.justock_api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class AdministratorUpdateRequest {

    @Email(message = "O campo email não é válido.")
    @Size(max = 255, message = "O campo email deve ter no máximo {max} caracteres.")
    private String email;

    @Size(min = 8, message = "O campo senha deve ter no mínimo {min} caracteres.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).*$",
        message = "A senha deve conter ao menos uma letra maiúscula, uma letra minúscula, um número e um símbolo."
    )
    private String password;
    private String passwordConfirmation;

    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String passwordConfirmation) { this.passwordConfirmation = passwordConfirmation; }
}
