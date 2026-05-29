package com.justeam.justock_api.repository;

import com.justeam.justock_api.model.MarketplaceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Long> {
    Optional<MarketplaceListing> findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(
            Integer usuario,
            String marketplaceSource,
            String marketplaceResourceId);

    List<MarketplaceListing> findByUsuarioAndMarketplaceSource(Integer usuario, String marketplaceSource);

    List<MarketplaceListing> findByUsuario(Integer usuario);

    List<MarketplaceListing> findByUsuarioAndProdutoVinculadoId(Integer usuario, Integer produtoVinculadoId);

    boolean existsByUsuarioAndProdutoVinculadoId(Integer usuario, Integer produtoVinculadoId);
}