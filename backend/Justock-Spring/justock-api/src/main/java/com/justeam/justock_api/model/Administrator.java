package com.justeam.justock_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "administrador")
@Data
public class Administrator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_administrador")
    private int idAdministrador;

    @Column(nullable = false, unique = true, length = 100)
    private String emailCorporativo;

    @Column(nullable = false)
    private String senha;

}
