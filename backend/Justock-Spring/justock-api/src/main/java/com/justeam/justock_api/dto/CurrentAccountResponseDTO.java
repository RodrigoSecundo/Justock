package com.justeam.justock_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurrentAccountResponseDTO {
    private Integer id;
    private Integer dashboardUserId;
    private String email;
    private String name;
    private String numero;
    private String role;
    private boolean primaryAdmin;
}