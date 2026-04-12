package com.justeam.justock_api.repository;

import com.justeam.justock_api.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
	Optional<Order> findByMarketplaceResourceIdAndMarketplaceSource(String marketplaceResourceId, String marketplaceSource);
	List<Order> findByMarketplaceSource(String marketplaceSource);
}
