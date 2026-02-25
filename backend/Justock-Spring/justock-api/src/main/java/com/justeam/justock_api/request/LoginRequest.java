package com.justeam.justock_api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "O campo email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "O campo senha é obrigatório")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
}
