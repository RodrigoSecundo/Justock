package com.justeam.justock_api.repository;

import com.justeam.justock_api.model.UserMarketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMarketplaceRepository extends JpaRepository<UserMarketplace, Integer> {
}
