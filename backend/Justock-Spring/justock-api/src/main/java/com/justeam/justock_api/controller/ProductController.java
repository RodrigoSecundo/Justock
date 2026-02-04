package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.ProductResponseDTO;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.request.ProductCreateRequest;
import com.justeam.justock_api.request.ProductUpdateRequest;
import com.justeam.justock_api.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    // GET /api/products
    @GetMapping("/")
    public ApiResponseDTO<List<ProductResponseDTO>> index() {
        List<ProductResponseDTO> products = productService.listAllProducts()
                .stream()
                .map(p -> new ProductResponseDTO(p.getIdProduto(), p.getCategoria(), p.getMarca(), p.getNomeDoProduto(), p.getEstado(), p.getPreco(), p.getCodigoDeBarras(), p.getQuantidade(), p.getQuantidadeReservada(), p.getMarcador(), p.getUsuario()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "Produtos encontrados!", products);
    }

    // GET /api/products/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    public ApiResponseDTO<ProductResponseDTO> show(@PathVariable Integer id) {
        Product product = productService.findProduct(id);
        if (product == null) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        ProductResponseDTO dto = new ProductResponseDTO(product.getIdProduto(), product.getCategoria(), product.getMarca(), product.getNomeDoProduto(), product.getEstado(), product.getPreco(), product.getCodigoDeBarras(), product.getQuantidade(), product.getQuantidadeReservada(), product.getMarcador(), product.getUsuario());
        return new ApiResponseDTO<>(200, "Produto encontrado!", dto);
    }

    // POST /api/products/cadastrar
    @PostMapping("/cadastrar")
    public ApiResponseDTO<ProductResponseDTO> store(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productService.createProduct(request);
        ProductResponseDTO dto = new ProductResponseDTO(product.getIdProduto(), product.getCategoria(), product.getMarca(), product.getNomeDoProduto(), product.getEstado(), product.getPreco(), product.getCodigoDeBarras(), product.getQuantidade(), product.getQuantidadeReservada(), product.getMarcador(), product.getUsuario());
        return new ApiResponseDTO<>(200, "Produto cadastrado com sucesso!", dto);
    }

    // PUT /api/products/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    public ApiResponseDTO<ProductResponseDTO> update(@PathVariable Integer id, @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productService.updateProduct(id, request);
        if (product == null) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        ProductResponseDTO dto = new ProductResponseDTO(product.getIdProduto(), product.getCategoria(), product.getMarca(), product.getNomeDoProduto(), product.getEstado(), product.getPreco(), product.getCodigoDeBarras(), product.getQuantidade(), product.getQuantidadeReservada(), product.getMarcador(), product.getUsuario());
        return new ApiResponseDTO<>(200, "Produto atualizado!", dto);
    }

    // DELETE /api/products/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    public ApiResponseDTO<Void> destroy(@PathVariable Integer id) {
        boolean deleted = productService.deleteProduct(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "Produto deletado!", null);
    }
}
