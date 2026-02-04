package com.justeam.justock_api.service;

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

    @Autowired
    private ProductRepository productRepository;

    public List<Product> listAllProducts() {
        return productRepository.findAll();
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
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Integer id, ProductUpdateRequest dto) {
        Product existingProduct = productRepository.findById(id).orElse(null);
        if (existingProduct != null) {
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
            return productRepository.save(existingProduct);
        }
        return null;
    }

    public boolean deleteProduct(Integer id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
