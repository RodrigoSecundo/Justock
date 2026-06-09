package com.justeam.justock_api.service;

import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.repository.MarketplaceListingRepository;
import com.justeam.justock_api.repository.OrderItemRepository;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.request.ProductUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MarketplaceListingRepository marketplaceListingRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private DashboardEventService dashboardEventService;

    @Mock
    private MercadoLivreService mercadoLivreService;

    @InjectMocks
    private ProductService productService;

    @Test
    void updateProductSyncsLinkedListingWhenStockChanges() {
        Product product = new Product();
        product.setIdProduto(42);
        product.setUsuario(1);
        product.setCategoria("GPU");
        product.setMarca("NVIDIA");
        product.setNomeDoProduto("RTX 3060");
        product.setEstado("ATIVO");
        product.setPreco(BigDecimal.valueOf(1999.90));
        product.setCodigoDeBarras("123");
        product.setQuantidade(0);
        product.setQuantidadeReservada(0);
        product.setMarcador("MANUAL");

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setQuantidade(3);

        when(productRepository.findById(42)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.updateProduct(42, request);

        assertEquals(3, updated.getQuantidade());
        verify(mercadoLivreService).syncManualInventoryForProduct(updated);
    }

    @Test
    void updateProductDoesNotSyncLinkedListingWhenStockWasNotTouched() {
        Product product = new Product();
        product.setIdProduto(42);
        product.setUsuario(1);
        product.setCategoria("GPU");
        product.setMarca("NVIDIA");
        product.setNomeDoProduto("RTX 3060");
        product.setEstado("ATIVO");
        product.setPreco(BigDecimal.valueOf(1999.90));
        product.setCodigoDeBarras("123");
        product.setQuantidade(2);
        product.setQuantidadeReservada(0);
        product.setMarcador("MANUAL");

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setPreco(BigDecimal.valueOf(1899.90));

        when(productRepository.findById(42)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.updateProduct(42, request);

        assertEquals(BigDecimal.valueOf(1899.90), updated.getPreco());
        verify(mercadoLivreService, never()).syncManualInventoryForProduct(any(Product.class));
    }
}