package com.justeam.justock_api.repository;

import com.justeam.justock_api.model.UserMarketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMarketplaceRepository extends JpaRepository<UserMarketplace, Integer> {
    List<UserMarketplace> findByUsuario(Integer usuario);
    Optional<UserMarketplace> findFirstByUsuarioAndMarketplaceId(Integer usuario, Integer marketplaceId);
    Optional<UserMarketplace> findFirstByIdLojaAndMarketplaceId(String idLoja, Integer marketplaceId);
}
