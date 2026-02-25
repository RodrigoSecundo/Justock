package com.justeam.justock_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponseDTO<T> {
    private int status;
    private String message;
    private T data;
}
