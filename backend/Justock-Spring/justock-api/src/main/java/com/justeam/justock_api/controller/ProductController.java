package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.ProductResponseDTO;
import com.justeam.justock_api.exception.BadRequestException;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.request.ProductCreateRequest;
import com.justeam.justock_api.request.ProductUpdateRequest;
import com.justeam.justock_api.service.CurrentAccountService;
import com.justeam.justock_api.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CurrentAccountService currentAccountService;

    // GET /api/products
    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<ProductResponseDTO>> index() {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        List<ProductResponseDTO> products = productService.listProductsByUsuario(usuarioId)
                .stream()
            .map(p -> new ProductResponseDTO(p.getIdProduto(), p.getCategoria(), p.getMarca(), p.getNomeDoProduto(), p.getEstado(), p.getPreco(), p.getCodigoDeBarras(), p.getQuantidade(), p.getQuantidadeReservada(), p.getMarcador(), p.getUsuario(), p.getMarketplaceResourceId(), p.getMarketplaceSource()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "Produtos encontrados!", products);
    }

    // GET /api/products/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<ProductResponseDTO> show(@PathVariable Integer id) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        Product product = productService.findProduct(id);
        if (product == null || !usuarioId.equals(product.getUsuario())) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        ProductResponseDTO dto = new ProductResponseDTO(product.getIdProduto(), product.getCategoria(), product.getMarca(), product.getNomeDoProduto(), product.getEstado(), product.getPreco(), product.getCodigoDeBarras(), product.getQuantidade(), product.getQuantidadeReservada(), product.getMarcador(), product.getUsuario(), product.getMarketplaceResourceId(), product.getMarketplaceSource());
        return new ApiResponseDTO<>(200, "Produto encontrado!", dto);
    }

    // POST /api/products/cadastrar
    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<ProductResponseDTO> store(@Valid @RequestBody ProductCreateRequest request) {
        request.setUsuario(currentAccountService.getDashboardUserId());
        Product product = productService.createProduct(request);
        ProductResponseDTO dto = new ProductResponseDTO(product.getIdProduto(), product.getCategoria(), product.getMarca(), product.getNomeDoProduto(), product.getEstado(), product.getPreco(), product.getCodigoDeBarras(), product.getQuantidade(), product.getQuantidadeReservada(), product.getMarcador(), product.getUsuario(), product.getMarketplaceResourceId(), product.getMarketplaceSource());
        return new ApiResponseDTO<>(200, "Produto cadastrado com sucesso!", dto);
    }

    // POST /api/products/importar
    @PostMapping("/importar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<List<ProductResponseDTO>> importBatch(@Valid @RequestBody List<@Valid ProductCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Nenhum produto válido foi enviado para importação.");
        }

        Integer usuarioId = currentAccountService.getDashboardUserId();
        requests.forEach(request -> request.setUsuario(usuarioId));

        List<ProductResponseDTO> products = productService.createProducts(requests)
                .stream()
                .map(product -> new ProductResponseDTO(product.getIdProduto(), product.getCategoria(), product.getMarca(), product.getNomeDoProduto(), product.getEstado(), product.getPreco(), product.getCodigoDeBarras(), product.getQuantidade(), product.getQuantidadeReservada(), product.getMarcador(), product.getUsuario(), product.getMarketplaceResourceId(), product.getMarketplaceSource()))
                .collect(Collectors.toList());

        return new ApiResponseDTO<>(200, "Produtos importados com sucesso!", products);
    }

    // PUT /api/products/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<ProductResponseDTO> update(@PathVariable Integer id, @Valid @RequestBody ProductUpdateRequest request) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        Product existingProduct = productService.findProduct(id);
        if (existingProduct == null || !usuarioId.equals(existingProduct.getUsuario())) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        Product product = productService.updateProduct(id, request);
        if (product == null) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        ProductResponseDTO dto = new ProductResponseDTO(product.getIdProduto(), product.getCategoria(), product.getMarca(), product.getNomeDoProduto(), product.getEstado(), product.getPreco(), product.getCodigoDeBarras(), product.getQuantidade(), product.getQuantidadeReservada(), product.getMarcador(), product.getUsuario(), product.getMarketplaceResourceId(), product.getMarketplaceSource());
        return new ApiResponseDTO<>(200, "Produto atualizado!", dto);
    }

    // DELETE /api/products/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<Void> destroy(@PathVariable Integer id) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        Product existingProduct = productService.findProduct(id);
        if (existingProduct == null || !usuarioId.equals(existingProduct.getUsuario())) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        boolean deleted = productService.deleteProduct(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "Produto não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "Produto deletado!", null);
    }
}
