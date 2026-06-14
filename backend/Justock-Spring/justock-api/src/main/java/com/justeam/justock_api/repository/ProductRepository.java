package com.justeam.justock_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.justeam.justock_api.model.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
	List<Product> findByUsuario(Integer usuario);
	List<Product> findByUsuarioAndMarketplaceSource(Integer usuario, String marketplaceSource);
	Optional<Product> findByMarketplaceResourceIdAndUsuario(String marketplaceResourceId, Integer usuario);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		UPDATE Product product
		   SET product.quantidade = product.quantidade - :amount
		 WHERE product.idProduto = :productId
		   AND product.quantidade >= :amount
		""")
	int decrementQuantityIfEnough(@Param("productId") Integer productId, @Param("amount") int amount);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		UPDATE Product product
		   SET product.quantidade = product.quantidade + :amount
		 WHERE product.idProduto = :productId
		""")
	int incrementQuantity(@Param("productId") Integer productId, @Param("amount") int amount);
}
