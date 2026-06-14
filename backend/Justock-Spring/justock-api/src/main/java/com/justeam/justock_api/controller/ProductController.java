package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.ProductResponseDTO;
import com.justeam.justock_api.exception.BadRequestException;
import com.justeam.justock_api.model.MarketplaceListing;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.request.MarketplaceListingLinkRequest;
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
        List<ProductResponseDTO> products = productService.listCatalogByUsuario(usuarioId);
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
        ProductResponseDTO dto = productService.toInternalProductDto(product, 0);
        return new ApiResponseDTO<>(200, "Produto encontrado!", dto);
    }

    // POST /api/products/cadastrar
    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<ProductResponseDTO> store(@Valid @RequestBody ProductCreateRequest request) {
        request.setUsuario(currentAccountService.getDashboardUserId());
        Product product = productService.createProduct(request);
        ProductResponseDTO dto = productService.toInternalProductDto(product, 0);
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
            .map(product -> productService.toInternalProductDto(product, 0))
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
        ProductResponseDTO dto = productService.toInternalProductDto(product, 0);
        return new ApiResponseDTO<>(200, "Produto atualizado!", dto);
    }

    @PostMapping("/anuncios/{listingId}/vincular")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<ProductResponseDTO> linkListing(@PathVariable Long listingId, @RequestBody MarketplaceListingLinkRequest request) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        MarketplaceListing listing = productService.linkMarketplaceListing(listingId, request.getIdProduto(), usuarioId);
        Product linkedProduct = request.getIdProduto() == null ? null : productService.findProduct(request.getIdProduto());
        ProductResponseDTO dto = productService.toMarketplaceListingDto(listing, linkedProduct, null);
        return new ApiResponseDTO<>(200, "Anúncio vinculado com sucesso!", dto);
    }

    @PostMapping("/anuncios/{listingId}/manter-separado")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<ProductResponseDTO> keepListingSeparated(@PathVariable Long listingId) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        MarketplaceListing listing = productService.keepListingSeparated(listingId, usuarioId);
        ProductResponseDTO dto = productService.toMarketplaceListingDto(listing, null, null);
        return new ApiResponseDTO<>(200, "Anúncio mantido separado com sucesso!", dto);
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
