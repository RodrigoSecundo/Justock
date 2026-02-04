package com.justeam.justock_api.service;

import com.justeam.justock_api.model.UserMarketplace;
import com.justeam.justock_api.repository.UserMarketplaceRepository;
import com.justeam.justock_api.request.UserMarketplaceCreateRequest;
import com.justeam.justock_api.request.UserMarketplaceUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class UserMarketplaceService {

    @Autowired
    private UserMarketplaceRepository userMarketplaceRepository;

    public List<UserMarketplace> listAllUserMarketplaces() {
        return userMarketplaceRepository.findAll();
    }

    public UserMarketplace findUserMarketplace(int id) {
        return userMarketplaceRepository.findById(id).orElse(null);
    }

    public UserMarketplace createUserMarketplace(UserMarketplaceCreateRequest dto) {
        UserMarketplace userMarketplace = new UserMarketplace();
        userMarketplace.setUsuario(dto.getUsuario());
        userMarketplace.setMarketplaceId(dto.getMarketplaceId());
        userMarketplace.setIdLoja(dto.getIdLoja());
        userMarketplace.setNomeLoja(dto.getNomeLoja());
        userMarketplace.setClienteId(dto.getClienteId());
        userMarketplace.setClienteSecret(dto.getClienteSecret());
        userMarketplace.setAccessToken(dto.getAccessToken());
        userMarketplace.setRefreshToken(dto.getRefreshToken());
        userMarketplace.setTokenExpiration(dto.getTokenExpiration());
        userMarketplace.setStatusIntegracao(dto.getStatusIntegracao());
        return userMarketplaceRepository.save(userMarketplace);
    }

    @Transactional
    public UserMarketplace updateUserMarketplace(int id, UserMarketplaceUpdateRequest dto) {
        UserMarketplace existingUserMarketplace = userMarketplaceRepository.findById(id).orElse(null);
        if (existingUserMarketplace != null) {
            existingUserMarketplace.setUsuario(dto.getUsuario());
            existingUserMarketplace.setMarketplaceId(dto.getMarketplaceId());
            existingUserMarketplace.setIdLoja(dto.getIdLoja());
            existingUserMarketplace.setNomeLoja(dto.getNomeLoja());
            existingUserMarketplace.setClienteId(dto.getClienteId());
            existingUserMarketplace.setClienteSecret(dto.getClienteSecret());
            existingUserMarketplace.setAccessToken(dto.getAccessToken());
            existingUserMarketplace.setRefreshToken(dto.getRefreshToken());
            existingUserMarketplace.setTokenExpiration(dto.getTokenExpiration());
            existingUserMarketplace.setStatusIntegracao(dto.getStatusIntegracao());
            return userMarketplaceRepository.save(existingUserMarketplace);
        }
        return null;
    }

    public boolean deleteUserMarketplace(int id) {
        if (userMarketplaceRepository.existsById(id)) {
            userMarketplaceRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
