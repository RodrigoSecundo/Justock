package com.justeam.justock_api.service;

import com.justeam.justock_api.exception.BadRequestException;
import com.justeam.justock_api.model.MarketplaceListing;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.repository.MarketplaceListingRepository;
import com.justeam.justock_api.repository.OrderItemRepository;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.dto.ProductResponseDTO;
import com.justeam.justock_api.request.ProductCreateRequest;
import com.justeam.justock_api.request.ProductUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.transaction.Transactional;

@Service
public class ProductService {

    public static final String LISTING_INVENTORY_MARKER = "ANUNCIO_ESTOQUE";

    private final ProductRepository productRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final OrderItemRepository orderItemRepository;
    private final DashboardEventService dashboardEventService;

    public ProductService(
            ProductRepository productRepository,
            MarketplaceListingRepository marketplaceListingRepository,
            OrderItemRepository orderItemRepository,
            DashboardEventService dashboardEventService) {
        this.productRepository = productRepository;
        this.marketplaceListingRepository = marketplaceListingRepository;
        this.orderItemRepository = orderItemRepository;
        this.dashboardEventService = dashboardEventService;
    }

    public List<Product> listAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> listProductsByUsuario(Integer usuarioId) {
        return productRepository.findByUsuario(usuarioId);
    }

    public List<ProductResponseDTO> listCatalogByUsuario(Integer usuarioId) {
        List<Product> allProducts = productRepository.findByUsuario(usuarioId);
        List<Product> internalProducts = allProducts.stream()
                .filter(product -> !isLegacyMarketplaceProduct(product))
                .filter(product -> !isListingInventoryShadow(product))
                .sorted(Comparator.comparing(Product::getIdProduto))
                .toList();
        Map<String, Product> shadowProductsByMarketplaceId = new HashMap<>();
        for (Product product : allProducts) {
            if (isListingInventoryShadow(product) && product.getMarketplaceResourceId() != null) {
                shadowProductsByMarketplaceId.put(product.getMarketplaceResourceId(), product);
            }
        }
        List<MarketplaceListing> listings = marketplaceListingRepository.findByUsuario(usuarioId)
                .stream()
                .sorted(Comparator.comparing(MarketplaceListing::getId))
                .toList();

        Map<Integer, Product> productById = new HashMap<>();
        Map<Integer, Integer> linkedListingCountByProduct = new HashMap<>();

        for (Product product : internalProducts) {
            productById.put(product.getIdProduto(), product);
        }

        for (MarketplaceListing listing : listings) {
            Integer linkedProductId = listing.getProdutoVinculadoId();
            if (linkedProductId != null) {
                linkedListingCountByProduct.merge(linkedProductId, 1, Integer::sum);
            }
        }

        List<ProductResponseDTO> rows = new ArrayList<>();
        for (Product product : internalProducts) {
            rows.add(toInternalProductDto(product, linkedListingCountByProduct.getOrDefault(product.getIdProduto(), 0)));
        }
        for (MarketplaceListing listing : listings) {
            rows.add(toMarketplaceListingDto(
                    listing,
                    productById.get(listing.getProdutoVinculadoId()),
                    shadowProductsByMarketplaceId.get(listing.getMarketplaceResourceId())));
        }

        return rows;
    }

    public Product findProduct(Integer id) {
        return productRepository.findById(id).orElse(null);
    }

    public Product createProduct(ProductCreateRequest dto) {
        Product savedProduct = saveProduct(dto);
        dashboardEventService.recordProductCreated(savedProduct);
        return savedProduct;
    }

    public List<Product> listLinkableProductsByUsuario(Integer usuarioId) {
        return productRepository.findByUsuario(usuarioId)
                .stream()
                .filter(product -> !isLegacyMarketplaceProduct(product))
                .filter(product -> !isListingInventoryShadow(product))
                .sorted(Comparator.comparing(Product::getNomeDoProduto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public List<Product> createProducts(List<ProductCreateRequest> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("Nenhum produto válido foi enviado para importação.");
        }

        List<Product> createdProducts = new ArrayList<>();
        for (ProductCreateRequest dto : dtos) {
            createdProducts.add(saveProduct(dto));
        }

        Integer usuario = createdProducts.isEmpty() ? null : createdProducts.get(0).getUsuario();
        if (usuario != null) {
            dashboardEventService.recordProductImported(usuario, createdProducts);
        }

        return createdProducts;
    }

    private Product saveProduct(ProductCreateRequest dto) {
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
            ensureProductHasNoLinkedListings(existingProduct);
            productRepository.deleteById(id);
            dashboardEventService.recordProductDeleted(existingProduct);
            return true;
        }
        return false;
    }

    @Transactional
    public MarketplaceListing linkMarketplaceListing(Long listingId, Integer productId, Integer usuarioId) {
        if (productId == null) {
            throw new BadRequestException("Selecione um produto para concluir a vinculação.");
        }

        Product product = productRepository.findById(productId)
                .filter(existing -> usuarioId.equals(existing.getUsuario()))
                .orElseThrow(() -> new BadRequestException("Produto selecionado não encontrado para este usuário."));
        ensureManualProduct(product);

        MarketplaceListing listing = marketplaceListingRepository.findById(listingId)
                .filter(existing -> usuarioId.equals(existing.getUsuario()))
                .orElseThrow(() -> new BadRequestException("Anúncio não encontrado para este usuário."));

        listing.setProdutoVinculadoId(productId);
        listing.setSeparadoManualmente(Boolean.FALSE);
        return marketplaceListingRepository.save(listing);
    }

    @Transactional
    public MarketplaceListing keepListingSeparated(Long listingId, Integer usuarioId) {
        MarketplaceListing listing = marketplaceListingRepository.findById(listingId)
                .filter(existing -> usuarioId.equals(existing.getUsuario()))
                .orElseThrow(() -> new BadRequestException("Anúncio não encontrado para este usuário."));

        listing.setProdutoVinculadoId(null);
        listing.setSeparadoManualmente(Boolean.TRUE);
        return marketplaceListingRepository.save(listing);
    }

    public ProductResponseDTO toInternalProductDto(Product product, int linkedListingCount) {
        ProductResponseDTO dto = new ProductResponseDTO(
                product.getIdProduto(),
                product.getCategoria(),
                product.getMarca(),
                product.getNomeDoProduto(),
                product.getEstado(),
                product.getPreco(),
                product.getCodigoDeBarras(),
                product.getQuantidade(),
                product.getQuantidadeReservada(),
                product.getMarcador(),
                product.getUsuario(),
                product.getMarketplaceResourceId(),
                product.getMarketplaceSource());
        dto.setTipoRegistro("PRODUTO");
        dto.setStatusVinculo(linkedListingCount > 0 ? "CORRETO" : "NAO_VINCULADO");
        dto.setProdutoVinculadoId(product.getIdProduto());
        dto.setProdutoVinculadoNome(product.getNomeDoProduto());
        dto.setInventoryProductId(product.getIdProduto());
        dto.setQuantidadeAnunciosVinculados(linkedListingCount);
        return dto;
    }

    public ProductResponseDTO toMarketplaceListingDto(MarketplaceListing listing, Product linkedProduct, Product shadowInventoryProduct) {
        Product inventoryProduct = linkedProduct != null ? linkedProduct : shadowInventoryProduct;
        String categoria = isMeaningfulCatalogValue(listing.getCategoria())
            ? listing.getCategoria()
            : inventoryProduct == null ? listing.getCategoria() : inventoryProduct.getCategoria();
        String marca = isMeaningfulCatalogValue(listing.getMarca())
            ? listing.getMarca()
            : inventoryProduct == null ? listing.getMarca() : inventoryProduct.getMarca();
        String codigoDeBarras = isMeaningfulCatalogValue(listing.getCodigoDeBarras())
            ? listing.getCodigoDeBarras()
            : inventoryProduct == null ? listing.getCodigoDeBarras() : inventoryProduct.getCodigoDeBarras();
        Integer quantidade = inventoryProduct == null ? listing.getQuantidadeDisponivel() : inventoryProduct.getQuantidade();
        ProductResponseDTO dto = new ProductResponseDTO(
                listing.getId() == null ? null : Math.toIntExact(listing.getId()),
            categoria,
            marca,
                listing.getTitulo(),
                "ATIVO",
                listing.getPreco(),
            codigoDeBarras,
            quantidade,
                0,
                "ML",
                listing.getUsuario(),
                listing.getMarketplaceResourceId(),
                listing.getMarketplaceSource());
        dto.setTipoRegistro("ANUNCIO");
        dto.setStatusVinculo(resolveListingStatus(listing));
        dto.setProdutoVinculadoId(linkedProduct == null ? null : linkedProduct.getIdProduto());
        dto.setProdutoVinculadoNome(linkedProduct == null ? null : linkedProduct.getNomeDoProduto());
        dto.setInventoryProductId(linkedProduct != null
            ? linkedProduct.getIdProduto()
            : shadowInventoryProduct == null ? null : shadowInventoryProduct.getIdProduto());
        dto.setQuantidadeAnunciosVinculados(linkedProduct == null ? 0 : 1);
        dto.setPermiteEscolherProduto(Boolean.TRUE);
        dto.setPermiteManterSeparado(Boolean.TRUE);
        return dto;
    }

    private boolean isMeaningfulCatalogValue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return !normalized.isBlank() && !"N/A".equalsIgnoreCase(normalized);
    }

    private void ensureManualProduct(Product product) {
        if (isLegacyMarketplaceProduct(product)) {
            throw new BadRequestException("Produtos sincronizados de marketplace não podem ser editados ou excluídos manualmente.");
        }
    }

    private boolean isLegacyMarketplaceProduct(Product product) {
        return (product.getMarketplaceSource() != null && !product.getMarketplaceSource().isBlank())
                || (product.getMarketplaceResourceId() != null && !product.getMarketplaceResourceId().isBlank())
                || "ML".equalsIgnoreCase(product.getMarcador());
    }

    private boolean isListingInventoryShadow(Product product) {
        return LISTING_INVENTORY_MARKER.equalsIgnoreCase(String.valueOf(product.getMarcador()));
    }

    private void ensureProductHasNoLinkedListings(Product product) {
        if (marketplaceListingRepository.existsByUsuarioAndProdutoVinculadoId(product.getUsuario(), product.getIdProduto())) {
            throw new BadRequestException("Desvincule os anúncios associados antes de excluir este produto.");
        }
        if (orderItemRepository.existsByIdIdProduto(product.getIdProduto())) {
            throw new BadRequestException("Este produto já possui pedidos vinculados e não pode ser excluído.");
        }
    }

    private String resolveListingStatus(MarketplaceListing listing) {
        if (listing.getProdutoVinculadoId() != null) {
            return "CORRETO";
        }
        if (Boolean.TRUE.equals(listing.getSeparadoManualmente())) {
            return "SEPARADO";
        }
        return "NAO_VINCULADO";
    }
}
