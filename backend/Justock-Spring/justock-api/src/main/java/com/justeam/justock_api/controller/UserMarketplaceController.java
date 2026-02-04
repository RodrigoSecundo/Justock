package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.UserMarketplaceResponseDTO;
import com.justeam.justock_api.model.UserMarketplace;
import com.justeam.justock_api.request.UserMarketplaceCreateRequest;
import com.justeam.justock_api.request.UserMarketplaceUpdateRequest;
import com.justeam.justock_api.service.UserMarketplaceService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/UserMarketplace")
@CrossOrigin(origins = "*")
public class UserMarketplaceController {

    @Autowired
    private UserMarketplaceService userMarketplaceService;

    // GET /api/UserMarketplace
    @GetMapping("/")
    public ApiResponseDTO<List<UserMarketplaceResponseDTO>> index() {
        List<UserMarketplaceResponseDTO> userMarketplaces = userMarketplaceService.listAllUserMarketplaces()
                .stream()
                .map(um -> new UserMarketplaceResponseDTO(um.getUsuarioMarketplaceId(), um.getUsuario(), um.getMarketplaceId(), um.getIdLoja(), um.getNomeLoja(), um.getClienteId(), um.getClienteSecret(), um.getAccessToken(), um.getRefreshToken(), um.getTokenExpiration(), um.getStatusIntegracao()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "UserMarketplaces encontrados!", userMarketplaces);
    }

    // GET /api/UserMarketplace/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    public ApiResponseDTO<UserMarketplaceResponseDTO> show(@PathVariable int id) {
        UserMarketplace userMarketplace = userMarketplaceService.findUserMarketplace(id);
        if (userMarketplace == null) {
            return new ApiResponseDTO<>(404, "UserMarketplace não encontrado!", null);
        }
        UserMarketplaceResponseDTO dto = new UserMarketplaceResponseDTO(userMarketplace.getUsuarioMarketplaceId(), userMarketplace.getUsuario(), userMarketplace.getMarketplaceId(), userMarketplace.getIdLoja(), userMarketplace.getNomeLoja(), userMarketplace.getClienteId(), userMarketplace.getClienteSecret(), userMarketplace.getAccessToken(), userMarketplace.getRefreshToken(), userMarketplace.getTokenExpiration(), userMarketplace.getStatusIntegracao());
        return new ApiResponseDTO<>(200, "UserMarketplace encontrado!", dto);
    }

    // POST /api/UserMarketplace/cadastrar
    @PostMapping("/cadastrar")
    public ApiResponseDTO<UserMarketplaceResponseDTO> store(@Valid @RequestBody UserMarketplaceCreateRequest request) {
        UserMarketplace userMarketplace = userMarketplaceService.createUserMarketplace(request);
        UserMarketplaceResponseDTO dto = new UserMarketplaceResponseDTO(userMarketplace.getUsuarioMarketplaceId(), userMarketplace.getUsuario(), userMarketplace.getMarketplaceId(), userMarketplace.getIdLoja(), userMarketplace.getNomeLoja(), userMarketplace.getClienteId(), userMarketplace.getClienteSecret(), userMarketplace.getAccessToken(), userMarketplace.getRefreshToken(), userMarketplace.getTokenExpiration(), userMarketplace.getStatusIntegracao());
        return new ApiResponseDTO<>(200, "UserMarketplace cadastrado com sucesso!", dto);
    }

    // PUT /api/UserMarketplace/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    public ApiResponseDTO<UserMarketplaceResponseDTO> update(@PathVariable int id, @Valid @RequestBody UserMarketplaceUpdateRequest request) {
        UserMarketplace userMarketplace = userMarketplaceService.updateUserMarketplace(id, request);
        if (userMarketplace == null) {
            return new ApiResponseDTO<>(404, "UserMarketplace não encontrado!", null);
        }
        UserMarketplaceResponseDTO dto = new UserMarketplaceResponseDTO(userMarketplace.getUsuarioMarketplaceId(), userMarketplace.getUsuario(), userMarketplace.getMarketplaceId(), userMarketplace.getIdLoja(), userMarketplace.getNomeLoja(), userMarketplace.getClienteId(), userMarketplace.getClienteSecret(), userMarketplace.getAccessToken(), userMarketplace.getRefreshToken(), userMarketplace.getTokenExpiration(), userMarketplace.getStatusIntegracao());
        return new ApiResponseDTO<>(200, "UserMarketplace atualizado!", dto);
    }

    // DELETE /api/UserMarketplace/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    public ApiResponseDTO<Void> destroy(@PathVariable int id) {
        boolean deleted = userMarketplaceService.deleteUserMarketplace(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "UserMarketplace não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "UserMarketplace deletado!", null);
    }
}
