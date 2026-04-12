package com.justeam.justock_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.justeam.justock_api.model.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
	List<Product> findByUsuario(Integer usuario);
	List<Product> findByUsuarioAndMarketplaceSource(Integer usuario, String marketplaceSource);
	Optional<Product> findByMarketplaceResourceIdAndUsuario(String marketplaceResourceId, Integer usuario);
}
