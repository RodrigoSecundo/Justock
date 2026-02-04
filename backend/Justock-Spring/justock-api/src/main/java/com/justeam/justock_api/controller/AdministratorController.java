package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.AdministratorResponseDTO;
import com.justeam.justock_api.model.Administrator;
import com.justeam.justock_api.request.AdministratorCreateRequest;
import com.justeam.justock_api.request.AdministratorUpdateRequest;
import com.justeam.justock_api.service.AdministratorService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/Administrator")
@CrossOrigin(origins = "*")
public class AdministratorController {

    @Autowired
    private AdministratorService administratorService;

    // GET /api/Administrator
    @GetMapping("/")
    public ApiResponseDTO<List<AdministratorResponseDTO>> index() {
        List<AdministratorResponseDTO> administrators = administratorService.listAllAdministrators()
                .stream()
                .map(a -> new AdministratorResponseDTO(a.getIdAdministrador(), a.getEmailCorporativo(), a.getSenha()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "Administradores encontrados!", administrators);
    }

    // GET /api/Administrator/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    public ApiResponseDTO<AdministratorResponseDTO> show(@PathVariable int id) {
        Administrator administrator = administratorService.findAdministrator(id);
        if (administrator == null) {
            return new ApiResponseDTO<>(404, "Administrador não encontrado!", null);
        }
        AdministratorResponseDTO dto = new AdministratorResponseDTO(administrator.getIdAdministrador(), administrator.getEmailCorporativo(), administrator.getSenha());
        return new ApiResponseDTO<>(200, "Administrador encontrado!", dto);
    }

    // POST /api/Administrator/cadastrar
    @PostMapping("/cadastrar")
    public ApiResponseDTO<AdministratorResponseDTO> store(@Valid @RequestBody AdministratorCreateRequest request) {
        Administrator administrator = administratorService.createAdministrator(request);
        AdministratorResponseDTO dto = new AdministratorResponseDTO(administrator.getIdAdministrador(), administrator.getEmailCorporativo(), administrator.getSenha());
        return new ApiResponseDTO<>(200, "Administrador cadastrado com sucesso!", dto);
    }

    // PUT /api/Administrator/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    public ApiResponseDTO<AdministratorResponseDTO> update(@PathVariable int id, @Valid @RequestBody AdministratorUpdateRequest request) {
        Administrator administrator = administratorService.updateAdministrator(id, request);
        if (administrator == null) {
            return new ApiResponseDTO<>(404, "Administrador não encontrado!", null);
        }
        AdministratorResponseDTO dto = new AdministratorResponseDTO(administrator.getIdAdministrador(), administrator.getEmailCorporativo(), administrator.getSenha());
        return new ApiResponseDTO<>(200, "Administrador atualizado!", dto);
    }

    // DELETE /api/Administrator/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    public ApiResponseDTO<Void> destroy(@PathVariable int id) {
        boolean deleted = administratorService.deleteAdministrator(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "Administrador não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "Administrador deletado!", null);
    }
}
