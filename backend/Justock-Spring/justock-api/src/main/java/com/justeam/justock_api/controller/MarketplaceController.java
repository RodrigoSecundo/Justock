package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.MarketplaceResponseDTO;
import com.justeam.justock_api.model.Marketplace;
import com.justeam.justock_api.request.MarketplaceCreateRequest;
import com.justeam.justock_api.request.MarketplaceUpdateRequest;
import com.justeam.justock_api.service.MarketplaceService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/Marketplace")
@CrossOrigin(origins = "*")
public class MarketplaceController {

    @Autowired
    private MarketplaceService marketplaceService;

    // GET /api/Marketplace
    @GetMapping("/")
    public ApiResponseDTO<List<MarketplaceResponseDTO>> index() {
        List<MarketplaceResponseDTO> marketplaces = marketplaceService.listAllMarketplaces()
                .stream()
                .map(m -> new MarketplaceResponseDTO(m.getMarketplaceId(), m.getNomeMarketplace(), m.getApiUrl(), m.getCreatedAt(), m.getUpdatedAt()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "Marketplaces encontrados!", marketplaces);
    }

    // GET /api/Marketplace/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    public ApiResponseDTO<MarketplaceResponseDTO> show(@PathVariable int id) {
        Marketplace marketplace = marketplaceService.findMarketplace(id);
        if (marketplace == null) {
            return new ApiResponseDTO<>(404, "Marketplace não encontrado!", null);
        }
        MarketplaceResponseDTO dto = new MarketplaceResponseDTO(marketplace.getMarketplaceId(), marketplace.getNomeMarketplace(), marketplace.getApiUrl(), marketplace.getCreatedAt(), marketplace.getUpdatedAt());
        return new ApiResponseDTO<>(200, "Marketplace encontrado!", dto);
    }

    // POST /api/Marketplace/cadastrar
    @PostMapping("/cadastrar")
    public ApiResponseDTO<MarketplaceResponseDTO> store(@Valid @RequestBody MarketplaceCreateRequest request) {
        Marketplace marketplace = marketplaceService.createMarketplace(request);
        MarketplaceResponseDTO dto = new MarketplaceResponseDTO(marketplace.getMarketplaceId(), marketplace.getNomeMarketplace(), marketplace.getApiUrl(), marketplace.getCreatedAt(), marketplace.getUpdatedAt());
        return new ApiResponseDTO<>(200, "Marketplace cadastrado com sucesso!", dto);
    }

    // PUT /api/Marketplace/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    public ApiResponseDTO<MarketplaceResponseDTO> update(@PathVariable int id, @Valid @RequestBody MarketplaceUpdateRequest request) {
        Marketplace marketplace = marketplaceService.updateMarketplace(id, request);
        if (marketplace == null) {
            return new ApiResponseDTO<>(404, "Marketplace não encontrado!", null);
        }
        MarketplaceResponseDTO dto = new MarketplaceResponseDTO(marketplace.getMarketplaceId(), marketplace.getNomeMarketplace(), marketplace.getApiUrl(), marketplace.getCreatedAt(), marketplace.getUpdatedAt());
        return new ApiResponseDTO<>(200, "Marketplace atualizado!", dto);
    }

    // DELETE /api/Marketplace/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    public ApiResponseDTO<Void> destroy(@PathVariable int id) {
        boolean deleted = marketplaceService.deleteMarketplace(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "Marketplace não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "Marketplace deletado!", null);
    }
}
