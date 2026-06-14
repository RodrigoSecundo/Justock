package com.justeam.justock_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.justock_api.model.MarketplaceListing;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.model.UserMarketplace;
import com.justeam.justock_api.model.WebhookEvent;
import com.justeam.justock_api.model.event.MercadoLivreWebhookReceivedEvent;
import com.justeam.justock_api.repository.MarketplaceListingRepository;
import com.justeam.justock_api.repository.OrderItemRepository;
import com.justeam.justock_api.repository.OrderRepository;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.repository.UserMarketplaceRepository;
import com.justeam.justock_api.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoLivreServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserMarketplaceRepository userMarketplaceRepository;

    @Mock
    private ProductRepository productRepository;

        @Mock
        private MarketplaceListingRepository marketplaceListingRepository;

    @Mock
    private OrderRepository orderRepository;

        @Mock
        private OrderItemRepository orderItemRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private DashboardEventService dashboardEventService;

    @Mock
    private ObjectMapper objectMapper;

        @Mock
        private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private MercadoLivreService mercadoLivreService;

        @org.junit.jupiter.api.BeforeEach
        void configureRetryDefaults() {
                ReflectionTestUtils.setField(mercadoLivreService, "retryMaxAttempts", 3);
                ReflectionTestUtils.setField(mercadoLivreService, "retryInitialBackoffMs", 0L);
        }

    @Test
    void syncInventoryUsesMarketplaceBarcodeInsteadOfItemId() {
        UserMarketplace connection = new UserMarketplace();
        connection.setUsuario(1);
        connection.setMarketplaceId(1);
        connection.setIdLoja("SELLER-1");
        connection.setAccessToken("token");
        connection.setRefreshToken("refresh");
        connection.setTokenExpiration(LocalDateTime.now().plusHours(1));
        connection.setClienteId("client");
        connection.setClienteSecret("secret");
        connection.setNomeLoja("Loja teste");
        connection.setStatusIntegracao("CONECTADO");

        when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
        when(restTemplate.exchange(
                eq("https://api.mercadolibre.com/users/SELLER-1/items/search"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("results", List.of("MLB123")), HttpStatus.OK));
        when(restTemplate.exchange(
                eq("https://api.mercadolibre.com/items?ids=MLB123"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)))
                .thenReturn(new ResponseEntity<>(List.of(Map.of(
                        "code", 200,
                        "body", Map.of(
                                "id", "MLB123",
                                "title", "Produto teste",
                                "price", 99.9,
                                "available_quantity", 7,
                                "category_id", "MLB1648",
                                "attributes", List.of(
                                        Map.of("id", "BRAND", "value_name", "Marca X"),
                                        Map.of("id", "EAN", "value_name", "7891234567890"))))),
                        HttpStatus.OK));
        when(marketplaceListingRepository.findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(1, "MERCADO_LIVRE", "MLB123"))
                .thenReturn(Optional.empty());
        when(marketplaceListingRepository.findByUsuarioAndMarketplaceSource(1, "MERCADO_LIVRE")).thenReturn(List.of());
        when(productRepository.findByUsuario(1)).thenReturn(List.of());
        when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = mercadoLivreService.syncInventory(1);

        assertEquals(1, processed);

        ArgumentCaptor<MarketplaceListing> listingCaptor = ArgumentCaptor.forClass(MarketplaceListing.class);
        verify(marketplaceListingRepository).save(listingCaptor.capture());
        verify(dashboardEventService, times(1)).recordSyncedMarketplaceListing(any(MarketplaceListing.class), eq(true));
        assertEquals("7891234567890", listingCaptor.getValue().getCodigoDeBarras());
    }

        @Test
        void syncInventoryUsesNaWhenMarketplaceBarcodeIsMissing() {
                UserMarketplace connection = new UserMarketplace();
                connection.setUsuario(1);
                connection.setMarketplaceId(1);
                connection.setIdLoja("SELLER-1");
                connection.setAccessToken("token");
                connection.setRefreshToken("refresh");
                connection.setTokenExpiration(LocalDateTime.now().plusHours(1));
                connection.setClienteId("client");
                connection.setClienteSecret("secret");
                connection.setNomeLoja("Loja teste");
                connection.setStatusIntegracao("CONECTADO");

                when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
                when(restTemplate.exchange(
                                eq("https://api.mercadolibre.com/users/SELLER-1/items/search"),
                                eq(HttpMethod.GET),
                                any(HttpEntity.class),
                                eq(Map.class)))
                                .thenReturn(new ResponseEntity<>(Map.of("results", List.of("MLB123")), HttpStatus.OK));
                when(restTemplate.exchange(
                                eq("https://api.mercadolibre.com/items?ids=MLB123"),
                                eq(HttpMethod.GET),
                                any(HttpEntity.class),
                                eq(List.class)))
                                .thenReturn(new ResponseEntity<>(List.of(Map.of(
                                                "code", 200,
                                                "body", Map.of(
                                                                "id", "MLB123",
                                                                "title", "Produto sem barcode",
                                                                "price", 99.9,
                                                                "available_quantity", 7,
                                                                "category_id", "MLB1648",
                                                                "attributes", List.of(
                                                                                Map.of("id", "BRAND", "value_name", "Marca X"))))),
                                                HttpStatus.OK));
                when(marketplaceListingRepository.findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(1, "MERCADO_LIVRE", "MLB123"))
                                .thenReturn(Optional.empty());
                when(marketplaceListingRepository.findByUsuarioAndMarketplaceSource(1, "MERCADO_LIVRE")).thenReturn(List.of());
                when(productRepository.findByUsuario(1)).thenReturn(List.of());
                when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

                int processed = mercadoLivreService.syncInventory(1);

                assertEquals(1, processed);

                ArgumentCaptor<MarketplaceListing> listingCaptor = ArgumentCaptor.forClass(MarketplaceListing.class);
                verify(marketplaceListingRepository).save(listingCaptor.capture());
                assertEquals("N/A", listingCaptor.getValue().getCodigoDeBarras());
        }

    @Test
    void syncInventoryInfersBrandFromTitleWhenMarketplaceBrandIsMissing() {
        UserMarketplace connection = new UserMarketplace();
        connection.setUsuario(1);
        connection.setMarketplaceId(1);
        connection.setIdLoja("SELLER-1");
        connection.setAccessToken("token");
        connection.setRefreshToken("refresh");
        connection.setTokenExpiration(LocalDateTime.now().plusHours(1));
        connection.setClienteId("client");
        connection.setClienteSecret("secret");
        connection.setNomeLoja("Loja teste");
        connection.setStatusIntegracao("CONECTADO");

        when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
        when(restTemplate.exchange(
                eq("https://api.mercadolibre.com/users/SELLER-1/items/search"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("results", List.of("MLB999")), HttpStatus.OK));
        when(restTemplate.exchange(
                eq("https://api.mercadolibre.com/items?ids=MLB999"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)))
                .thenReturn(new ResponseEntity<>(List.of(Map.of(
                        "code", 200,
                        "body", Map.of(
                                "id", "MLB999",
                                "title", "Memória Ram Ddr5 8gb 4800mhz Sodimm Netac Para Notebook",
                                "price", 99.9,
                                "available_quantity", 7,
                                "category_id", "MLB1648",
                                "attributes", List.of()))),
                        HttpStatus.OK));
        when(marketplaceListingRepository.findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(1, "MERCADO_LIVRE", "MLB999"))
                .thenReturn(Optional.empty());
        when(marketplaceListingRepository.findByUsuarioAndMarketplaceSource(1, "MERCADO_LIVRE")).thenReturn(List.of());
        when(productRepository.findByUsuario(1)).thenReturn(List.of());
        when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = mercadoLivreService.syncInventory(1);

        assertEquals(1, processed);

        ArgumentCaptor<MarketplaceListing> listingCaptor = ArgumentCaptor.forClass(MarketplaceListing.class);
        verify(marketplaceListingRepository).save(listingCaptor.capture());
        assertEquals("Netac", listingCaptor.getValue().getMarca());
    }

    @Test
    void syncInventoryDoesNotFailWhenTitleContainsRepeatedTokens() {
        UserMarketplace connection = new UserMarketplace();
        connection.setUsuario(1);
        connection.setMarketplaceId(1);
        connection.setIdLoja("SELLER-1");
        connection.setAccessToken("token");
        connection.setRefreshToken("refresh");
        connection.setTokenExpiration(LocalDateTime.now().plusHours(1));
        connection.setClienteId("client");
        connection.setClienteSecret("secret");
        connection.setNomeLoja("Loja teste");
        connection.setStatusIntegracao("CONECTADO");

        Product internalProduct = new Product();
        internalProduct.setIdProduto(77);
        internalProduct.setUsuario(1);
        internalProduct.setNomeDoProduto("Placa de video de teste");
        internalProduct.setMarca("Marca X");
        internalProduct.setCodigoDeBarras("N/A");
        internalProduct.setMarcador("MANUAL");

        when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
        when(restTemplate.exchange(
                eq("https://api.mercadolibre.com/users/SELLER-1/items/search"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("results", List.of("MLB123")), HttpStatus.OK));
        when(restTemplate.exchange(
                eq("https://api.mercadolibre.com/items?ids=MLB123"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)))
                .thenReturn(new ResponseEntity<>(List.of(Map.of(
                        "code", 200,
                        "body", Map.of(
                                "id", "MLB123",
                                "title", "Placa de video de de teste",
                                "price", 99.9,
                                "available_quantity", 0,
                                "category_id", "MLB1648",
                                "attributes", List.of(
                                        Map.of("id", "BRAND", "value_name", "Marca X"))))),
                        HttpStatus.OK));
        when(marketplaceListingRepository.findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(1, "MERCADO_LIVRE", "MLB123"))
                .thenReturn(Optional.empty());
        when(marketplaceListingRepository.findByUsuarioAndMarketplaceSource(1, "MERCADO_LIVRE")).thenReturn(List.of());
        when(productRepository.findByUsuario(1)).thenReturn(List.of(internalProduct));
        when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = mercadoLivreService.syncInventory(1);

        assertEquals(1, processed);
        verify(marketplaceListingRepository).save(any(MarketplaceListing.class));
    }

        @Test
        void syncManualInventoryForLinkedProductPausesListingWhenStockReachesZero() {
                UserMarketplace connection = new UserMarketplace();
                connection.setUsuario(1);
                connection.setMarketplaceId(1);
                connection.setIdLoja("SELLER-1");
                connection.setAccessToken("token");
                connection.setRefreshToken("refresh");
                connection.setTokenExpiration(LocalDateTime.now().plusHours(1));

                Product product = new Product();
                product.setIdProduto(42);
                product.setUsuario(1);
                product.setQuantidade(0);
                product.setMarcador("MANUAL");

                MarketplaceListing listing = new MarketplaceListing();
                listing.setId(7L);
                listing.setUsuario(1);
                listing.setMarketplaceSource("MERCADO_LIVRE");
                listing.setMarketplaceResourceId("MLB6616692878");
                listing.setProdutoVinculadoId(42);
                listing.setQuantidadeDisponivel(1);

                when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
                when(marketplaceListingRepository.findByUsuarioAndProdutoVinculadoId(1, 42)).thenReturn(List.of(listing));
                when(restTemplate.exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                any(HttpEntity.class),
                                eq(Map.class)))
                                .thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));
                when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

                mercadoLivreService.syncManualInventoryForProduct(product);

                ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
                verify(restTemplate).exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                requestCaptor.capture(),
                                eq(Map.class));
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) requestCaptor.getValue().getBody();
                assertEquals("paused", payload.get("status"));
                assertFalse(payload.containsKey("available_quantity"));

                ArgumentCaptor<MarketplaceListing> listingCaptor = ArgumentCaptor.forClass(MarketplaceListing.class);
                verify(marketplaceListingRepository).save(listingCaptor.capture());
                assertEquals(0, listingCaptor.getValue().getQuantidadeDisponivel());
        }

        @Test
        void syncManualInventoryForLinkedProductReactivatesListingWhenStockReturns() {
                UserMarketplace connection = new UserMarketplace();
                connection.setUsuario(1);
                connection.setMarketplaceId(1);
                connection.setIdLoja("SELLER-1");
                connection.setAccessToken("token");
                connection.setRefreshToken("refresh");
                connection.setTokenExpiration(LocalDateTime.now().plusHours(1));

                Product product = new Product();
                product.setIdProduto(42);
                product.setUsuario(1);
                product.setQuantidade(3);
                product.setMarcador("MANUAL");

                MarketplaceListing listing = new MarketplaceListing();
                listing.setId(7L);
                listing.setUsuario(1);
                listing.setMarketplaceSource("MERCADO_LIVRE");
                listing.setMarketplaceResourceId("MLB6616692878");
                listing.setProdutoVinculadoId(42);
                listing.setQuantidadeDisponivel(0);

                when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
                when(marketplaceListingRepository.findByUsuarioAndProdutoVinculadoId(1, 42)).thenReturn(List.of(listing));
                when(restTemplate.exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                any(HttpEntity.class),
                                eq(Map.class)))
                                .thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));
                when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

                mercadoLivreService.syncManualInventoryForProduct(product);

                ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
                verify(restTemplate).exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                requestCaptor.capture(),
                                eq(Map.class));
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) requestCaptor.getValue().getBody();
                assertEquals(3, payload.get("available_quantity"));
                assertEquals("active", payload.get("status"));
                assertTrue(payload.containsKey("available_quantity"));

                ArgumentCaptor<MarketplaceListing> listingCaptor = ArgumentCaptor.forClass(MarketplaceListing.class);
                verify(marketplaceListingRepository).save(listingCaptor.capture());
                assertEquals(3, listingCaptor.getValue().getQuantidadeDisponivel());
        }

        @Test
        void syncManualInventoryForLinkedProductKeepsListingActiveWhenStockIncreasesAboveOne() {
                UserMarketplace connection = new UserMarketplace();
                connection.setUsuario(1);
                connection.setMarketplaceId(1);
                connection.setIdLoja("SELLER-1");
                connection.setAccessToken("token");
                connection.setRefreshToken("refresh");
                connection.setTokenExpiration(LocalDateTime.now().plusHours(1));

                Product product = new Product();
                product.setIdProduto(42);
                product.setUsuario(1);
                product.setQuantidade(5);
                product.setMarcador("MANUAL");

                MarketplaceListing listing = new MarketplaceListing();
                listing.setId(7L);
                listing.setUsuario(1);
                listing.setMarketplaceSource("MERCADO_LIVRE");
                listing.setMarketplaceResourceId("MLB6616692878");
                listing.setProdutoVinculadoId(42);
                listing.setQuantidadeDisponivel(1);

                when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
                when(marketplaceListingRepository.findByUsuarioAndProdutoVinculadoId(1, 42)).thenReturn(List.of(listing));
                when(restTemplate.exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                any(HttpEntity.class),
                                eq(Map.class)))
                                .thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));
                when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

                mercadoLivreService.syncManualInventoryForProduct(product);

                ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
                verify(restTemplate).exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                requestCaptor.capture(),
                                eq(Map.class));
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) requestCaptor.getValue().getBody();
                assertEquals(5, payload.get("available_quantity"));
                assertEquals("active", payload.get("status"));

                ArgumentCaptor<MarketplaceListing> listingCaptor = ArgumentCaptor.forClass(MarketplaceListing.class);
                verify(marketplaceListingRepository).save(listingCaptor.capture());
                assertEquals(5, listingCaptor.getValue().getQuantidadeDisponivel());
        }

        @Test
        void syncManualInventoryForLinkedProductFallsBackToMarketplaceMaxQuantity() {
                UserMarketplace connection = new UserMarketplace();
                connection.setUsuario(1);
                connection.setMarketplaceId(1);
                connection.setIdLoja("SELLER-1");
                connection.setAccessToken("token");
                connection.setRefreshToken("refresh");
                connection.setTokenExpiration(LocalDateTime.now().plusHours(1));

                Product product = new Product();
                product.setIdProduto(42);
                product.setUsuario(1);
                product.setQuantidade(5);
                product.setMarcador("MANUAL");

                MarketplaceListing listing = new MarketplaceListing();
                listing.setId(7L);
                listing.setUsuario(1);
                listing.setMarketplaceSource("MERCADO_LIVRE");
                listing.setMarketplaceResourceId("MLB6616692878");
                listing.setProdutoVinculadoId(42);
                listing.setQuantidadeDisponivel(1);

                HttpClientErrorException invalidQuantity = HttpClientErrorException.create(
                                HttpStatus.BAD_REQUEST,
                                "Bad Request",
                                HttpHeaders.EMPTY,
                                "{\"cause\":[{\"code\":\"item.available_quantity.invalid\",\"message\":\"Available quantity max. value is 1 for category MLB1658\"}]}".getBytes(),
                                null);

                when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
                when(marketplaceListingRepository.findByUsuarioAndProdutoVinculadoId(1, 42)).thenReturn(List.of(listing));
                doThrow(invalidQuantity)
                                .doReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK))
                                .when(restTemplate)
                                .exchange(
                                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                                eq(HttpMethod.PUT),
                                                any(HttpEntity.class),
                                                eq(Map.class));
                when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

                mercadoLivreService.syncManualInventoryForProduct(product);

                ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
                verify(restTemplate, times(2)).exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                requestCaptor.capture(),
                                eq(Map.class));
                @SuppressWarnings("unchecked")
                Map<String, Object> firstPayload = (Map<String, Object>) requestCaptor.getAllValues().get(0).getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> secondPayload = (Map<String, Object>) requestCaptor.getAllValues().get(1).getBody();
                assertEquals(5, firstPayload.get("available_quantity"));
                assertEquals(1, secondPayload.get("available_quantity"));
                assertEquals("active", secondPayload.get("status"));

                ArgumentCaptor<MarketplaceListing> listingCaptor = ArgumentCaptor.forClass(MarketplaceListing.class);
                verify(marketplaceListingRepository).save(listingCaptor.capture());
                assertEquals(1, listingCaptor.getValue().getQuantidadeDisponivel());
        }

        @Test
        void syncManualInventoryRetriesTransientMarketplaceFailure() {
                UserMarketplace connection = new UserMarketplace();
                connection.setUsuario(1);
                connection.setMarketplaceId(1);
                connection.setIdLoja("SELLER-1");
                connection.setAccessToken("token");
                connection.setRefreshToken("refresh");
                connection.setTokenExpiration(LocalDateTime.now().plusHours(1));

                Product product = new Product();
                product.setIdProduto(42);
                product.setUsuario(1);
                product.setQuantidade(2);
                product.setMarcador("MANUAL");

                MarketplaceListing listing = new MarketplaceListing();
                listing.setId(7L);
                listing.setUsuario(1);
                listing.setMarketplaceSource("MERCADO_LIVRE");
                listing.setMarketplaceResourceId("MLB6616692878");
                listing.setProdutoVinculadoId(42);
                listing.setQuantidadeDisponivel(1);

                HttpServerErrorException transientFailure = HttpServerErrorException.create(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Service Unavailable",
                                HttpHeaders.EMPTY,
                                "temporarily unavailable".getBytes(),
                                null);

                when(userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(1, 1)).thenReturn(Optional.of(connection));
                when(marketplaceListingRepository.findByUsuarioAndProdutoVinculadoId(1, 42)).thenReturn(List.of(listing));
                doThrow(transientFailure)
                                .doReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK))
                                .when(restTemplate)
                                .exchange(
                                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                                eq(HttpMethod.PUT),
                                                any(HttpEntity.class),
                                                eq(Map.class));
                when(marketplaceListingRepository.save(any(MarketplaceListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

                mercadoLivreService.syncManualInventoryForProduct(product);

                verify(restTemplate, times(2)).exchange(
                                eq("https://api.mercadolibre.com/items/MLB6616692878"),
                                eq(HttpMethod.PUT),
                                any(HttpEntity.class),
                                eq(Map.class));
        }

        @Test
        void registerWebhookEventQueuesKnownUserForAsyncProcessing() {
                UserMarketplace connection = new UserMarketplace();
                connection.setUsuario(1);
                connection.setUsuarioMarketplaceId(10);
                connection.setMarketplaceId(1);
                connection.setIdLoja("SELLER-1");
                connection.setAccessToken("token");
                connection.setRefreshToken("refresh");
                connection.setTokenExpiration(LocalDateTime.now().plusHours(1));

                when(userMarketplaceRepository.findFirstByIdLojaAndMarketplaceId("SELLER-1", 1)).thenReturn(Optional.of(connection));
                AtomicInteger generatedId = new AtomicInteger(99);
                when(webhookEventRepository.save(any())).thenAnswer(invocation -> {
                        WebhookEvent event = invocation.getArgument(0);
                        if (event.getId() == 0) {
                                event.setId(generatedId.get());
                        }
                        return event;
                });

                mercadoLivreService.registerWebhookEvent(Map.of("user_id", "SELLER-1", "topic", "orders_v2"));

                ArgumentCaptor<WebhookEvent> eventCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
                verify(webhookEventRepository).save(eventCaptor.capture());
                assertFalse(Boolean.TRUE.equals(eventCaptor.getValue().getProcessed()));
                assertEquals(null, eventCaptor.getValue().getProcessedAt());

                ArgumentCaptor<MercadoLivreWebhookReceivedEvent> publishedEvent = ArgumentCaptor.forClass(MercadoLivreWebhookReceivedEvent.class);
                verify(applicationEventPublisher).publishEvent(publishedEvent.capture());
                assertEquals(99, publishedEvent.getValue().webhookEventId());
                assertEquals(1, publishedEvent.getValue().usuarioId());
        }

        @Test
        void registerWebhookEventStoresErrorWhenMarketplaceUserIsUnknown() {
                when(webhookEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                mercadoLivreService.registerWebhookEvent(Map.of("user_id", "UNKNOWN", "topic", "orders_v2"));

                ArgumentCaptor<WebhookEvent> eventCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
                verify(webhookEventRepository).save(eventCaptor.capture());
                assertEquals("Usuario marketplace nao encontrado para o webhook recebido.", eventCaptor.getValue().getError());
        }
}