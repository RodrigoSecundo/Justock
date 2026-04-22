package com.justeam.justock_api.service;

import com.justeam.justock_api.exception.BadRequestException;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.request.ProductCreateRequest;
import com.justeam.justock_api.request.ProductUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final DashboardEventService dashboardEventService;

    public ProductService(ProductRepository productRepository, DashboardEventService dashboardEventService) {
        this.productRepository = productRepository;
        this.dashboardEventService = dashboardEventService;
    }

    public List<Product> listAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> listProductsByUsuario(Integer usuarioId) {
        return productRepository.findByUsuario(usuarioId);
    }

    public Product findProduct(Integer id) {
        return productRepository.findById(id).orElse(null);
    }

    public Product createProduct(ProductCreateRequest dto) {
        Product product = new Product();
        product.setCategoria(dto.getCategoria());
        product.setMarca(dto.getMarca());
        product.setNomeDoProduto(dto.getNomeDoProduto());
        product.setEstado(dto.getEstado());
        product.setPreco(dto.getPreco());
        product.setCodigoDeBarras(dto.getCodigoDeBarras());
        product.setQuantidade(dto.getQuantidade());
        product.setQuantidadeReservada(dto.getQuantidadeReservada());
        product.setMarcador(dto.getMarcador());
        product.setUsuario(dto.getUsuario());
        Product savedProduct = productRepository.save(product);
        dashboardEventService.recordProductCreated(savedProduct);
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Integer id, ProductUpdateRequest dto) {
        Product existingProduct = productRepository.findById(id).orElse(null);
        if (existingProduct != null) {
            ensureManualProduct(existingProduct);
            Product previousProduct = new Product();
            previousProduct.setIdProduto(existingProduct.getIdProduto());
            previousProduct.setNomeDoProduto(existingProduct.getNomeDoProduto());
            previousProduct.setQuantidade(existingProduct.getQuantidade());
            previousProduct.setUsuario(existingProduct.getUsuario());
            if (dto.getCategoria() != null) existingProduct.setCategoria(dto.getCategoria());
            if (dto.getMarca() != null) existingProduct.setMarca(dto.getMarca());
            if (dto.getNomeDoProduto() != null) existingProduct.setNomeDoProduto(dto.getNomeDoProduto());
            if (dto.getEstado() != null) existingProduct.setEstado(dto.getEstado());
            if (dto.getPreco() != null) existingProduct.setPreco(dto.getPreco());
            if (dto.getCodigoDeBarras() != null) existingProduct.setCodigoDeBarras(dto.getCodigoDeBarras());
            if (dto.getQuantidade() != null) existingProduct.setQuantidade(dto.getQuantidade());
            if (dto.getQuantidadeReservada() != null) existingProduct.setQuantidadeReservada(dto.getQuantidadeReservada());
            if (dto.getMarcador() != null) existingProduct.setMarcador(dto.getMarcador());
            if (dto.getUsuario() != null) existingProduct.setUsuario(dto.getUsuario());
            Product updatedProduct = productRepository.save(existingProduct);
            dashboardEventService.recordProductUpdated(previousProduct, updatedProduct);
            return updatedProduct;
        }
        return null;
    }

    public boolean deleteProduct(Integer id) {
        Product existingProduct = productRepository.findById(id).orElse(null);
        if (existingProduct != null) {
            ensureManualProduct(existingProduct);
            productRepository.deleteById(id);
            dashboardEventService.recordProductDeleted(existingProduct);
            return true;
        }
        return false;
    }

    private void ensureManualProduct(Product product) {
        boolean marketplaceManaged = (product.getMarketplaceSource() != null && !product.getMarketplaceSource().isBlank())
                || (product.getMarketplaceResourceId() != null && !product.getMarketplaceResourceId().isBlank())
                || "ML".equalsIgnoreCase(product.getMarcador());

        if (marketplaceManaged) {
            throw new BadRequestException("Produtos sincronizados de marketplace não podem ser editados ou excluídos manualmente.");
        }
    }
}
