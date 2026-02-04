package com.justeam.justock_api.service;

import com.justeam.justock_api.model.Marketplace;
import com.justeam.justock_api.repository.MarketplaceRepository;
import com.justeam.justock_api.request.MarketplaceCreateRequest;
import com.justeam.justock_api.request.MarketplaceUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class MarketplaceService {

    @Autowired
    private MarketplaceRepository marketplaceRepository;

    public List<Marketplace> listAllMarketplaces() {
        return marketplaceRepository.findAll();
    }

    public Marketplace findMarketplace(int id) {
        return marketplaceRepository.findById(id).orElse(null);
    }

    public Marketplace createMarketplace(MarketplaceCreateRequest dto) {
        Marketplace marketplace = new Marketplace();
        marketplace.setNomeMarketplace(dto.getNomeMarketplace());
        marketplace.setApiUrl(dto.getApiUrl());
        marketplace.setCreatedAt(LocalDateTime.now());
        marketplace.setUpdatedAt(LocalDateTime.now());
        return marketplaceRepository.save(marketplace);
    }

    @Transactional
    public Marketplace updateMarketplace(int id, MarketplaceUpdateRequest dto) {
        Marketplace existingMarketplace = marketplaceRepository.findById(id).orElse(null);
        if (existingMarketplace != null) {
            existingMarketplace.setNomeMarketplace(dto.getNomeMarketplace());
            existingMarketplace.setApiUrl(dto.getApiUrl());
            existingMarketplace.setUpdatedAt(LocalDateTime.now());
            return marketplaceRepository.save(existingMarketplace);
        }
        return null;
    }

    public boolean deleteMarketplace(int id) {
        if (marketplaceRepository.existsById(id)) {
            marketplaceRepository.deleteById(id);
            return true;
        }
        return false;
    }

}
