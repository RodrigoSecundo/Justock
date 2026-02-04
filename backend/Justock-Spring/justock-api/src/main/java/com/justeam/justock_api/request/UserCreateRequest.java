package com.justeam.justock_api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class UserCreateRequest {

    @NotBlank(message = "O campo nome é obrigatório.")
    @Size(max = 50, message = "O campo nome deve ter no máximo {max} caracteres.")
    private String name;

    @NotBlank(message = "O campo email é obrigatório.")
    @Email(message = "O campo email não é válido.")
    @Size(max = 255, message = "O campo email deve ter no máximo {max} caracteres.")
    private String email;

    @NotBlank(message = "O campo senha é obrigatório.")
    @Size(min = 8, message = "O campo senha deve ter no mínimo {min} caracteres.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = "A senha deve conter ao menos uma letra maiúscula, uma letra minúscula, um número e um símbolo."
    )
    
    private String password;

    @NotBlank(message = "O campo confirmação de senha é obrigatório.")
    private String passwordConfirmation;

    private String numero;

    // Getters e Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String passwordConfirmation) { this.passwordConfirmation = passwordConfirmation; }

    public String getNumero() {return numero;}
    public void setNumero(String numero) {this.numero = numero;}
    
}
