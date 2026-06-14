package com.justeam.justock_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.justock_api.model.MarketplaceListing;
import com.justeam.justock_api.model.OrderItem;
import com.justeam.justock_api.model.OrderItemId;
import com.justeam.justock_api.model.Order;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class MercadoLivreService {

    private static final String MERCADO_LIVRE_SOURCE = "MERCADO_LIVRE";
    private static final Pattern MARKETPLACE_MAX_QUANTITY_PATTERN = Pattern.compile("max\\. value is (\\d+)", Pattern.CASE_INSENSITIVE);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ACTIVE_ORDER_STATUS_FILTER = String.join(",",
            "confirmed",
            "payment_required",
            "payment_in_process",
            "partially_paid",
            "paid");
    private static final Set<String> STOP_TOKENS = Set.of("de", "do", "da", "dos", "das", "para", "por", "com", "sem", "e", "a", "o", "um", "uma", "na", "no", "em");
    private static final Set<String> CRITICAL_MODEL_TOKENS = Set.of("ti", "super", "xt", "xtx", "oc", "mini", "pro", "max", "ultra", "8gb", "12gb", "16gb", "24gb", "lhr");
        private static final Map<String, String> NORMALIZED_BRAND_BY_TOKEN = Map.ofEntries(
            Map.entry("asus", "ASUS"),
            Map.entry("galax", "Galax"),
            Map.entry("gigabyte", "Gigabyte"),
            Map.entry("msi", "MSI"),
            Map.entry("kingston", "Kingston"),
            Map.entry("corsair", "Corsair"),
            Map.entry("samsung", "Samsung"),
            Map.entry("seagate", "Seagate"),
            Map.entry("netac", "Netac"),
            Map.entry("intel", "Intel"),
            Map.entry("amd", "AMD"),
            Map.entry("nvidia", "NVIDIA"),
            Map.entry("wd", "WD"),
            Map.entry("western digital", "WD"),
            Map.entry("g skill", "G.Skill"),
            Map.entry("gskill", "G.Skill"),
            Map.entry("xpg", "XPG"));

    @Value("${mercadolivre.client.id}")
    private String clientId;

    @Value("${mercadolivre.client.secret}")
    private String clientSecret;

    @Value("${mercadolivre.redirect.uri}")
    private String redirectUri;

    @Value("${mercadolivre.frontend.redirect-uri:http://localhost:5173/conexoes}")
    private String frontendRedirectUri;

    @Value("${mercadolivre.shared.usuario-id:1}")
    private Integer sharedUsuarioId;

    @Value("${mercadolivre.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${mercadolivre.retry.initial-backoff-ms:500}")
    private long retryInitialBackoffMs;

    @Value("${mercadolivre.rate-limit.min-interval-ms:250}")
    private long rateLimitMinIntervalMs;

    private final Object rateLimitMonitor = new Object();
    private long nextMarketplaceRequestAtMs = 0L;

    private final RestTemplate restTemplate;
    private final UserMarketplaceRepository userMarketplaceRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final DashboardEventService dashboardEventService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Map<String, String> pkceVerifierByState = new ConcurrentHashMap<>();
    private final Map<String, String> categoryNameCache = new ConcurrentHashMap<>();

    public MercadoLivreService(RestTemplate restTemplate, UserMarketplaceRepository userMarketplaceRepository,
        MarketplaceListingRepository marketplaceListingRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            WebhookEventRepository webhookEventRepository,
            DashboardEventService dashboardEventService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher) {
        this.restTemplate = restTemplate;
        this.userMarketplaceRepository = userMarketplaceRepository;
        this.marketplaceListingRepository = marketplaceListingRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.dashboardEventService = dashboardEventService;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Integer getIntegrationUserId() {
        return sharedUsuarioId;
    }

    public String getAuthorizationUrl(Integer usuarioId) {
        try {
            String state = buildOauthState(usuarioId);
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);

            pkceVerifierByState.put(state, codeVerifier);

            String encodedUri = java.net.URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name());
            String encodedState = java.net.URLEncoder.encode(state, StandardCharsets.UTF_8.name());
            return "https://auth.mercadolivre.com.br/authorization?response_type=code&client_id=" + clientId
                    + "&redirect_uri=" + encodedUri
                    + "&state=" + encodedState
                    + "&code_challenge=" + codeChallenge
                    + "&code_challenge_method=S256";
        } catch (Exception e) {
            return "";
        }
    }

    public String getFrontendRedirectUri() {
        return frontendRedirectUri;
    }

    public boolean isConnected(Integer usuarioId) {
        return userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(usuarioId, 1).isPresent();
    }

    public Map<String, Object> getConnectionSummary(Integer usuarioId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("connected", Boolean.FALSE);
        summary.put("sellerId", null);
        summary.put("totalVendas", 0);
        summary.put("pedidosAtivos", 0);
        summary.put("totalInventario", 0);

        Optional<UserMarketplace> connectionOpt = userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(usuarioId, 1);
        if (connectionOpt.isEmpty()) {
            return summary;
        }

        UserMarketplace connection = ensureValidAccessToken(connectionOpt.get());
        summary.put("connected", Boolean.TRUE);
        summary.put("sellerId", connection.getIdLoja());

        summary.put("totalVendas", safeCount(() -> fetchSellerCompletedSales(connection)));
        summary.put("pedidosAtivos", safeCount(() -> fetchActiveListings(connection)));
        summary.put("totalInventario", countSyncedInventoryQuantity(usuarioId));

        return summary;
    }

    public Integer extractUsuarioIdFromState(String state) {
        if (state == null || state.isBlank()) {
            throw new RuntimeException("State OAuth ausente.");
        }

        String[] parts = state.split(":", 2);
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("State OAuth inválido.", e);
        }
    }

    public void processCallback(String code, String state) {
        Integer usuarioId = extractUsuarioIdFromState(state);
        String codeVerifier = pkceVerifierByState.remove(state);
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new RuntimeException("code_verifier não encontrado para o callback OAuth. Refaça a conexão com o Mercado Livre.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

                ResponseEntity<Map> response = postForEntityWithRetry(
                    "oauth-token",
                    "https://api.mercadolibre.com/oauth/token",
                    request,
                    Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = response.getBody();
                String accessToken = (String) data.get("access_token");
                String refreshToken = (String) data.get("refresh_token");
                Number expiresIn = (Number) data.get("expires_in");
                String mlUserId = String.valueOf(data.get("user_id"));

                UserMarketplace um = userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(usuarioId, 1)
                    .orElseGet(UserMarketplace::new);
                um.setUsuario(usuarioId);
                um.setMarketplaceId(1);
                um.setAccessToken(accessToken);
                um.setRefreshToken(refreshToken);
                um.setTokenExpiration(LocalDateTime.now().plusSeconds(expiresIn.longValue()));
                um.setClienteId(clientId);
                um.setClienteSecret(clientSecret);
                um.setIdLoja(mlUserId);
                um.setNomeLoja("Mercado Livre - " + mlUserId);
                um.setStatusIntegracao("CONECTADO");

                userMarketplaceRepository.save(um);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            throw new RuntimeException("MercadoLivre API Error: " + errorBody);
        } catch (Exception e) {
            throw new RuntimeException("Internal Error processing ML Callback: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> syncMarketplaceData(Integer usuarioId) {
        int syncedProducts = syncInventory(usuarioId);
        int syncedOrders = syncOrders(usuarioId);
        dashboardEventService.recordSyncCompleted(usuarioId, syncedProducts, syncedOrders);
        return getConnectionSummary(usuarioId);
    }

    public int syncInventory(Integer usuarioId) {
        UserMarketplace um = ensureValidAccessToken(getConnectionOrThrow(usuarioId));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(um.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        String urlIds = "https://api.mercadolibre.com/users/" + um.getIdLoja() + "/items/search";
        ResponseEntity<Map> searchResponse = exchangeWithRetry(urlIds, HttpMethod.GET, request, Map.class,
            "inventory-search");

        if (searchResponse.getStatusCode().is2xxSuccessful() && searchResponse.getBody() != null) {
            List<String> itemsId = (List<String>) searchResponse.getBody().get("results");
            Set<String> syncedItemIds = new HashSet<>();
            int processedItems = 0;

            if (itemsId != null && !itemsId.isEmpty()) {
                syncedItemIds.addAll(itemsId);
                for (List<String> chunk : partition(itemsId, 20)) {
                    String idsParam = String.join(",", chunk);
                    String urlItems = "https://api.mercadolibre.com/items?ids=" + idsParam;
                        ResponseEntity<List> itemsResponse = exchangeWithRetry(urlItems, HttpMethod.GET, request,
                            List.class, "inventory-items-batch");

                    if (itemsResponse.getStatusCode().is2xxSuccessful() && itemsResponse.getBody() != null) {
                        List<Map<String, Object>> itemsList = itemsResponse.getBody();
                        for (Map<String, Object> itemWrapper : itemsList) {
                            if (Integer.valueOf(200).equals(itemWrapper.get("code"))) {
                                Map<String, Object> itemMap = (Map<String, Object>) itemWrapper.get("body");
                                saveOrUpdateListing(itemMap, usuarioId);
                                processedItems++;
                            }
                        }
                    }
                }
            }

            removeListingsMissingFromLatestSync(usuarioId, syncedItemIds);
            return processedItems;
        }

        return 0;
    }

    public int syncOrders(Integer usuarioId) {
        UserMarketplace connection = ensureValidAccessToken(getConnectionOrThrow(usuarioId));
        List<Map<String, Object>> orders = fetchOrders(connection, null);
        Set<String> syncedOrderIds = new HashSet<>();
        int processedOrders = 0;

        for (Map<String, Object> orderSummary : orders) {
            Object orderId = orderSummary.get("id");
            if (orderId == null) {
                continue;
            }

            String resourceId = String.valueOf(orderId);
            Map<String, Object> orderMap = fetchOrderDetails(connection, resourceId);
            syncedOrderIds.add(resourceId);
            saveOrUpdateOrder(orderMap, connection, resourceId);
            processedOrders++;
        }

        removeOrdersMissingFromLatestSync(syncedOrderIds);
        return processedOrders;
    }

    private void saveOrUpdateListing(Map<String, Object> itemMap, Integer usuarioId) {
        String mlId = (String) itemMap.get("id");
        String title = (String) itemMap.get("title");
        Number priceNum = (Number) itemMap.get("price");
        Number availableQty = (Number) itemMap.get("available_quantity");

        BigDecimal price = new BigDecimal(priceNum.toString());
        Integer quantity = availableQty.intValue();
        String brand = resolveNormalizedBrand(itemMap, title);
        String barcode = extractBarcode(itemMap);
        String category = resolveNormalizedCategory(itemMap, title);

        boolean created = marketplaceListingRepository
                .findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(usuarioId, "MERCADO_LIVRE", mlId)
                .isEmpty();
        MarketplaceListing listing = marketplaceListingRepository
                .findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(usuarioId, "MERCADO_LIVRE", mlId)
                .orElseGet(MarketplaceListing::new);
        listing.setUsuario(usuarioId);
        listing.setMarketplaceSource("MERCADO_LIVRE");
        listing.setMarketplaceResourceId(mlId);
        listing.setTitulo(title);
        listing.setCategoria(category);
        listing.setMarca(brand);
        listing.setCodigoDeBarras(barcode);
        listing.setPreco(price);
        listing.setQuantidadeDisponivel(quantity);
        if (listing.getSeparadoManualmente() == null) {
            listing.setSeparadoManualmente(Boolean.FALSE);
        }
        if (!Boolean.TRUE.equals(listing.getSeparadoManualmente()) && listing.getProdutoVinculadoId() == null) {
            autoLinkListing(listing, usuarioId);
        }

        MarketplaceListing savedListing = marketplaceListingRepository.save(listing);
        synchronizeShadowInventoryProduct(savedListing, quantity);
        dashboardEventService.recordSyncedMarketplaceListing(savedListing, created);
    }

    private MarketplaceListing ensureListingExistsForOrderItem(Map<?, ?> itemData, Map<?, ?> itemMap, Integer usuarioId) {
        Object itemIdRaw = itemData.get("id");
        if (itemIdRaw == null) {
            return null;
        }

        String itemId = String.valueOf(itemIdRaw);
        MarketplaceListing existingListing = marketplaceListingRepository
                .findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(usuarioId, "MERCADO_LIVRE", itemId)
                .orElse(null);
        if (existingListing != null) {
            return existingListing;
        }

        MarketplaceListing listing = new MarketplaceListing();
        listing.setUsuario(usuarioId);
        listing.setMarketplaceSource("MERCADO_LIVRE");
        listing.setMarketplaceResourceId(itemId);
        listing.setTitulo(itemData.get("title") == null ? itemId : String.valueOf(itemData.get("title")));
        listing.setCategoria(resolveNormalizedCategory((Map<String, Object>) itemData, listing.getTitulo()));
        listing.setMarca(resolveNormalizedBrand((Map<String, Object>) itemData, listing.getTitulo()));
        listing.setCodigoDeBarras(extractBarcode((Map<String, Object>) itemData));
        BigDecimal unitPrice = convertToBigDecimal(itemMap.get("unit_price"));
        listing.setPreco(unitPrice == null ? BigDecimal.ZERO : unitPrice);
        listing.setQuantidadeDisponivel(0);
        listing.setSeparadoManualmente(Boolean.FALSE);
        autoLinkListing(listing, usuarioId);

        MarketplaceListing savedListing = marketplaceListingRepository.save(listing);
        synchronizeShadowInventoryProduct(savedListing, 0);
        dashboardEventService.recordSyncedMarketplaceListing(savedListing, true);
        return savedListing;
    }

    private void saveOrUpdateOrder(Map<String, Object> orderMap, UserMarketplace connection, String resourceId) {
        boolean created = orderRepository.findByMarketplaceResourceIdAndMarketplaceSource(resourceId, "MERCADO_LIVRE").isEmpty();
        Order order = orderRepository.findByMarketplaceResourceIdAndMarketplaceSource(resourceId, "MERCADO_LIVRE")
            .orElseGet(Order::new);
        boolean inventoryWasApplied = Boolean.TRUE.equals(order.getInventoryApplied());

        order.setIdPedidoMarketplace(1);
        order.setUsuarioMarketplaceId(connection.getUsuarioMarketplaceId());
        order.setDataEmissao(parseMercadoLivreDate(orderMap.get("date_created")));
        order.setDataEntrega(null);
        order.setStatusPedido(mapMercadoLivreOrderStatus(orderMap));
        order.setStatusPagamento(mapMercadoLivrePaymentStatus(orderMap));
        order.setMarketplaceSource("MERCADO_LIVRE");
        order.setMarketplaceResourceId(resourceId);
        order.setObservacao(buildMarketplaceOrderObservation(orderMap, resourceId));
        if (order.getInventoryApplied() == null) {
            order.setInventoryApplied(Boolean.FALSE);
        }

        Order savedOrder = orderRepository.save(order);
        syncMarketplaceOrderItems(savedOrder, orderMap, connection.getUsuario(), inventoryWasApplied);
        dashboardEventService.recordSyncedOrder(savedOrder, created);
    }

    public void registerWebhookEvent(Map<String, Object> payload) {
        String userId = payload.get("user_id") == null ? null : String.valueOf(payload.get("user_id"));
        Optional<UserMarketplace> marketplaceOpt = userId == null
                ? Optional.empty()
                : userMarketplaceRepository.findFirstByIdLojaAndMarketplaceId(userId, 1);

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setUsuarioMarketplaceId(marketplaceOpt.map(UserMarketplace::getUsuarioMarketplaceId).orElse(null));
        webhookEvent.setMarketplaceId(1);
        webhookEvent.setTipoEvento(payload.get("topic") == null ? "mercadolivre" : String.valueOf(payload.get("topic")));
        webhookEvent.setPayload(toJson(payload));
        webhookEvent.setReceivedAt(LocalDateTime.now());
        webhookEvent.setProcessed(Boolean.FALSE);
        webhookEvent.setProcessedAt(null);
        webhookEvent.setProcessingStartedAt(null);
        webhookEvent.setAttemptCount(0);
        webhookEvent.setError(marketplaceOpt.isPresent() ? null : "Usuario marketplace nao encontrado para o webhook recebido.");

        WebhookEvent savedEvent = webhookEventRepository.save(webhookEvent);
        if (marketplaceOpt.isPresent()) {
            applicationEventPublisher.publishEvent(
                    new MercadoLivreWebhookReceivedEvent(savedEvent.getId(), marketplaceOpt.get().getUsuario()));
        }
    }

    public Integer resolveUsuarioIdFromWebhookEvent(Integer usuarioMarketplaceId) {
        if (usuarioMarketplaceId == null) {
            return null;
        }

        return userMarketplaceRepository.findById(usuarioMarketplaceId)
                .map(UserMarketplace::getUsuario)
                .orElse(null);
    }

    @org.springframework.transaction.annotation.Transactional
    public void disconnect(Integer usuarioId) {
        List<UserMarketplace> list = userMarketplaceRepository.findByUsuario(usuarioId);
        userMarketplaceRepository.deleteAll(list);
    }

    private void removeListingsMissingFromLatestSync(Integer usuarioId, Set<String> syncedItemIds) {
        List<MarketplaceListing> existingListings = marketplaceListingRepository.findByUsuarioAndMarketplaceSource(usuarioId, "MERCADO_LIVRE");

        for (MarketplaceListing listing : existingListings) {
            String resourceId = listing.getMarketplaceResourceId();
            if (resourceId == null || !syncedItemIds.contains(resourceId)) {
                listing.setQuantidadeDisponivel(0);
                marketplaceListingRepository.save(listing);
                synchronizeShadowInventoryProduct(listing, 0);
            }
        }
    }

    private void removeOrdersMissingFromLatestSync(Set<String> syncedOrderIds) {
        List<Order> existingOrders = orderRepository.findByMarketplaceSource("MERCADO_LIVRE");

        for (Order order : existingOrders) {
            String resourceId = order.getMarketplaceResourceId();
            if (resourceId == null || !syncedOrderIds.contains(resourceId)) {
                if (Boolean.TRUE.equals(order.getInventoryApplied())) {
                    applyInventoryAdjustment(orderItemRepository.findByIdIdPedido(order.getIdPedido()), 1, null);
                }
                orderItemRepository.deleteByIdIdPedido(order.getIdPedido());
                orderRepository.delete(order);
            }
        }
    }

    private void autoLinkListing(MarketplaceListing listing, Integer usuarioId) {
        List<Product> candidates = productRepository.findByUsuario(usuarioId)
                .stream()
                .filter(product -> !isLegacyMarketplaceProduct(product))
                .filter(product -> !isListingInventoryShadow(product))
                .toList();

        Product exactNameMatch = findUniqueExactNameMatch(listing, candidates);
        if (exactNameMatch != null) {
            listing.setProdutoVinculadoId(exactNameMatch.getIdProduto());
            return;
        }

        Product similarNameMatch = findSafeSimilarNameMatch(listing, candidates);
        if (similarNameMatch != null) {
            listing.setProdutoVinculadoId(similarNameMatch.getIdProduto());
            return;
        }

        Product barcodeMatch = findUniqueBarcodeMatch(listing, candidates);
        if (barcodeMatch != null) {
            listing.setProdutoVinculadoId(barcodeMatch.getIdProduto());
        }
    }

    private Product findUniqueBarcodeMatch(MarketplaceListing listing, List<Product> candidates) {
        String barcode = normalizeComparableBarcode(listing.getCodigoDeBarras());
        if (barcode == null) {
            return null;
        }

        List<Product> matches = candidates.stream()
                .filter(product -> barcode.equals(normalizeComparableBarcode(product.getCodigoDeBarras())))
                .toList();

        return matches.size() == 1 ? matches.get(0) : null;
    }

    private Product findUniqueExactNameMatch(MarketplaceListing listing, List<Product> candidates) {
        String normalizedTitle = normalizeText(listing.getTitulo());
        if (normalizedTitle.isBlank()) {
            return null;
        }

        List<Product> matches = candidates.stream()
                .filter(product -> normalizedTitle.equals(normalizeText(product.getNomeDoProduto())))
                .toList();

        if (matches.size() == 1) {
            return matches.get(0);
        }

        String normalizedBrand = normalizeText(listing.getMarca());
        if (normalizedBrand.isBlank()) {
            return null;
        }

        List<Product> brandMatches = matches.stream()
                .filter(product -> normalizedBrand.equals(normalizeText(product.getMarca())))
                .toList();
        return brandMatches.size() == 1 ? brandMatches.get(0) : null;
    }

    private Product findSafeSimilarNameMatch(MarketplaceListing listing, List<Product> candidates) {
        String normalizedTitle = normalizeText(listing.getTitulo());
        if (normalizedTitle.isBlank()) {
            return null;
        }

        double bestScore = 0d;
        double secondBestScore = 0d;
        Product bestProduct = null;
        for (Product candidate : candidates) {
            if (!hasCompatibleModelTokens(listing, candidate)) {
                continue;
            }
            double score = computeSimilarityScore(listing, candidate);
            if (score > bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                bestProduct = candidate;
            } else if (score > secondBestScore) {
                secondBestScore = score;
            }
        }

        if (bestProduct == null) {
            return null;
        }
        if (bestScore < 0.92d) {
            return null;
        }
        if ((bestScore - secondBestScore) < 0.08d) {
            return null;
        }
        return bestProduct;
    }

    private double computeSimilarityScore(MarketplaceListing listing, Product product) {
        String normalizedTitle = normalizeText(listing.getTitulo());
        String normalizedProductName = normalizeText(product.getNomeDoProduto());
        if (normalizedTitle.isBlank() || normalizedProductName.isBlank()) {
            return 0d;
        }
        if (normalizedTitle.equals(normalizedProductName)) {
            return 1d;
        }

        Set<String> titleTokens = tokenizeNormalizedText(normalizedTitle);
        Set<String> productTokens = tokenizeNormalizedText(normalizedProductName);
        int intersection = 0;
        for (String token : titleTokens) {
            if (productTokens.contains(token)) {
                intersection++;
            }
        }
        int union = titleTokens.size() + productTokens.size() - intersection;
        double jaccard = union == 0 ? 0d : (double) intersection / (double) union;

        double containment = 0d;
        if (normalizedTitle.contains(normalizedProductName) || normalizedProductName.contains(normalizedTitle)) {
            int maxLength = Math.max(normalizedTitle.length(), normalizedProductName.length());
            int minLength = Math.min(normalizedTitle.length(), normalizedProductName.length());
            containment = maxLength == 0 ? 0d : (double) minLength / (double) maxLength;
        }

        double brandBoost = normalizeText(listing.getMarca()).equals(normalizeText(product.getMarca())) ? 0.08d : 0d;
        return Math.max(jaccard, containment) + brandBoost;
    }

    private boolean hasCompatibleModelTokens(MarketplaceListing listing, Product product) {
        Set<String> listingTokens = extractModelTokens(normalizeText(listing.getTitulo()));
        Set<String> productTokens = extractModelTokens(normalizeText(product.getNomeDoProduto()));
        if (listingTokens.isEmpty() || productTokens.isEmpty()) {
            return true;
        }

        Set<String> listingOnly = new HashSet<>(listingTokens);
        listingOnly.removeAll(productTokens);
        Set<String> productOnly = new HashSet<>(productTokens);
        productOnly.removeAll(listingTokens);

        return listingOnly.stream().noneMatch(this::isCriticalModelToken)
                && productOnly.stream().noneMatch(this::isCriticalModelToken);
    }

    private Set<String> extractModelTokens(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return Set.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : tokenizeNormalizedText(normalizedText)) {
            if (STOP_TOKENS.contains(token)) {
                continue;
            }
            if (token.matches(".*\\d.*") || CRITICAL_MODEL_TOKENS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean isCriticalModelToken(String token) {
        return CRITICAL_MODEL_TOKENS.contains(token) || token.matches(".*\\d.*");
    }

    private Set<String> tokenizeNormalizedText(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(normalizedText.split(" "))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeComparableBarcode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9A-Za-z]", "").trim();
        if (normalized.isEmpty() || "N/A".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private boolean isLegacyMarketplaceProduct(Product product) {
        return (product.getMarketplaceSource() != null && !product.getMarketplaceSource().isBlank())
                || (product.getMarketplaceResourceId() != null && !product.getMarketplaceResourceId().isBlank())
                || "ML".equalsIgnoreCase(product.getMarcador());
    }

    private boolean isListingInventoryShadow(Product product) {
        return ProductService.LISTING_INVENTORY_MARKER.equalsIgnoreCase(String.valueOf(product.getMarcador()));
    }

    private Integer resolveInventoryProductId(MarketplaceListing listing) {
        if (listing == null) {
            return null;
        }
        if (listing.getProdutoVinculadoId() != null) {
            return listing.getProdutoVinculadoId();
        }
        Product shadowInventoryProduct = findShadowInventoryProduct(listing);
        return shadowInventoryProduct == null ? null : shadowInventoryProduct.getIdProduto();
    }

    private Product findShadowInventoryProduct(MarketplaceListing listing) {
        if (listing == null || listing.getMarketplaceResourceId() == null || listing.getUsuario() == null) {
            return null;
        }
        return productRepository.findByUsuario(listing.getUsuario())
                .stream()
                .filter(this::isListingInventoryShadow)
                .filter(product -> listing.getMarketplaceResourceId().equals(product.getMarketplaceResourceId()))
                .findFirst()
                .orElse(null);
    }

    private void synchronizeShadowInventoryProduct(MarketplaceListing listing, Integer quantity) {
        if (listing == null || listing.getMarketplaceResourceId() == null || listing.getUsuario() == null) {
            return;
        }
        if (listing.getProdutoVinculadoId() != null) {
            return;
        }

        Product product = findShadowInventoryProduct(listing);
        if (product == null) {
            product = new Product();
            product.setUsuario(listing.getUsuario());
            product.setMarcador(ProductService.LISTING_INVENTORY_MARKER);
            product.setMarketplaceSource(listing.getMarketplaceSource());
            product.setMarketplaceResourceId(listing.getMarketplaceResourceId());
            product.setQuantidadeReservada(0);
            product.setEstado("ATIVO");
        }

        product.setCategoria(listing.getCategoria() == null || listing.getCategoria().isBlank() ? "Marketplace" : listing.getCategoria());
        product.setMarca(listing.getMarca() == null || listing.getMarca().isBlank() ? "Marketplace" : listing.getMarca());
        product.setNomeDoProduto(listing.getTitulo());
        product.setPreco(listing.getPreco() == null ? BigDecimal.ZERO : listing.getPreco());
        product.setCodigoDeBarras(listing.getCodigoDeBarras() == null || listing.getCodigoDeBarras().isBlank() ? "N/A" : listing.getCodigoDeBarras());
        product.setQuantidade(quantity == null ? 0 : quantity);
        productRepository.save(product);
    }

    public void syncManualInventoryForProduct(Product product) {
        if (product == null || product.getUsuario() == null) {
            return;
        }

        if (isListingInventoryShadow(product)
                && "MERCADO_LIVRE".equalsIgnoreCase(product.getMarketplaceSource())
                && product.getMarketplaceResourceId() != null) {
            marketplaceListingRepository
                    .findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(product.getUsuario(), "MERCADO_LIVRE", product.getMarketplaceResourceId())
                    .ifPresent(listing -> syncListingQuantityToMarketplace(listing, product.getQuantidade() == null ? 0 : product.getQuantidade()));
            return;
        }

        for (MarketplaceListing listing : marketplaceListingRepository.findByUsuarioAndProdutoVinculadoId(product.getUsuario(), product.getIdProduto())) {
            syncListingQuantityToMarketplace(listing, product.getQuantidade() == null ? 0 : product.getQuantidade());
        }
    }

    private void syncListingQuantityToMarketplace(MarketplaceListing listing, int quantity) {
        if (listing == null || listing.getUsuario() == null) {
            return;
        }

        UserMarketplace connection = ensureValidAccessToken(getConnectionOrThrow(listing.getUsuario()));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        int safeQuantity = Math.max(quantity, 0);
        int appliedQuantity = safeQuantity;

        try {
            updateMarketplaceListingQuantity(listing, headers, safeQuantity);
        } catch (org.springframework.web.client.HttpClientErrorException exception) {
            Integer marketplaceLimit = resolveMarketplaceQuantityLimit(exception.getResponseBodyAsString());
            if (safeQuantity > 0 && marketplaceLimit != null && marketplaceLimit >= 0 && marketplaceLimit < safeQuantity) {
                appliedQuantity = marketplaceLimit;
                updateMarketplaceListingQuantity(listing, headers, appliedQuantity);
            } else {
                throw exception;
            }
        }

        listing.setQuantidadeDisponivel(appliedQuantity);
        marketplaceListingRepository.save(listing);
    }

    private void updateMarketplaceListingQuantity(MarketplaceListing listing, HttpHeaders headers, int quantity) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(buildMarketplaceQuantityPayload(quantity), headers);
        exchangeWithRetry(
                "https://api.mercadolibre.com/items/" + listing.getMarketplaceResourceId(),
                HttpMethod.PUT,
                request,
            Map.class,
            "listing-quantity-update");
    }

    private Map<String, Object> buildMarketplaceQuantityPayload(int quantity) {
        int safeQuantity = Math.max(quantity, 0);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (safeQuantity <= 0) {
            payload.put("status", "paused");
            return payload;
        }

        payload.put("available_quantity", safeQuantity);
        payload.put("status", "active");
        return payload;
    }

    private Integer resolveMarketplaceQuantityLimit(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            if (response == null) {
                return parseMarketplaceQuantityLimit(responseBody);
            }
            Object causesValue = response.get("cause");
            if (causesValue instanceof List<?> causes) {
                for (Object causeValue : causes) {
                    if (!(causeValue instanceof Map<?, ?> cause)) {
                        continue;
                    }

                    Object code = cause.get("code");
                    Object message = cause.get("message");
                    if (!"item.available_quantity.invalid".equals(String.valueOf(code))) {
                        continue;
                    }

                    Integer parsedLimit = parseMarketplaceQuantityLimit(String.valueOf(message));
                    if (parsedLimit != null) {
                        return parsedLimit;
                    }
                }
            }
        } catch (JsonProcessingException ignored) {
            // Fall back to a regex scan when Mercado Livre returns an unexpected body.
        }

        return parseMarketplaceQuantityLimit(responseBody);
    }

    private Integer parseMarketplaceQuantityLimit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = MARKETPLACE_MAX_QUANTITY_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void syncMarketplaceOrderItems(Order order, Map<String, Object> orderMap, Integer usuarioId, boolean inventoryWasApplied) {
        List<OrderItem> existingItems = orderItemRepository.findByIdIdPedido(order.getIdPedido());
        List<OrderItem> desiredItems = buildMarketplaceOrderItems(order, orderMap, usuarioId);

        boolean itemsChanged = !haveSameMarketplaceOrderItems(existingItems, desiredItems);
        if (itemsChanged) {
            if (inventoryWasApplied) {
                applyInventoryAdjustment(existingItems, 1, usuarioId);
                order.setInventoryApplied(Boolean.FALSE);
                orderRepository.save(order);
                inventoryWasApplied = false;
            }
            orderItemRepository.deleteByIdIdPedido(order.getIdPedido());
            for (OrderItem desiredItem : desiredItems) {
                orderItemRepository.save(desiredItem);
            }
        }

        reconcileMarketplaceOrderInventory(order, desiredItems, inventoryWasApplied, usuarioId);
    }

    private List<OrderItem> buildMarketplaceOrderItems(Order order, Map<String, Object> orderMap, Integer usuarioId) {
        Object rawItems = orderMap.get("order_items");
        if (!(rawItems instanceof List<?> orderItemsList)) {
            return List.of();
        }

        List<OrderItem> items = new ArrayList<>();
        for (Object rawItem : orderItemsList) {
            if (!(rawItem instanceof Map<?, ?> itemMap)) {
                continue;
            }
            Object itemDataObj = itemMap.get("item");
            if (!(itemDataObj instanceof Map<?, ?> itemData)) {
                continue;
            }

            Object itemIdRaw = itemData.get("id");
            if (itemIdRaw == null) {
                continue;
            }
            String itemId = String.valueOf(itemIdRaw);

                MarketplaceListing listing = marketplaceListingRepository
                    .findByUsuarioAndMarketplaceSourceAndMarketplaceResourceId(usuarioId, "MERCADO_LIVRE", itemId)
                    .orElseGet(() -> ensureListingExistsForOrderItem(itemData, itemMap, usuarioId));
            Integer inventoryProductId = resolveInventoryProductId(listing);
            if (listing == null || inventoryProductId == null) {
                continue;
            }

            Number quantityRaw = itemMap.get("quantity") instanceof Number quantity ? quantity : null;
            int quantity = quantityRaw == null ? 0 : quantityRaw.intValue();
            if (quantity <= 0) {
                continue;
            }

            BigDecimal unitPrice = convertToBigDecimal(itemMap.get("unit_price"));
            if (unitPrice == null) {
                unitPrice = listing.getPreco() == null ? BigDecimal.ZERO : listing.getPreco();
            }

            OrderItem orderItem = new OrderItem();
            OrderItemId id = new OrderItemId();
            id.setIdPedido(order.getIdPedido());
            id.setIdProduto(inventoryProductId);
            orderItem.setId(id);
            orderItem.setQuantidade(quantity);
            orderItem.setPrecoUnitario(unitPrice);
            orderItem.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP));
            orderItem.setIdItemMarketplace(itemId);
            orderItem.setItemStatus(itemMap.get("item_status") == null ? "ACTIVE" : String.valueOf(itemMap.get("item_status")));
            items.add(orderItem);
        }
        return items;
    }

    private boolean haveSameMarketplaceOrderItems(List<OrderItem> currentItems, List<OrderItem> desiredItems) {
        if (currentItems.size() != desiredItems.size()) {
            return false;
        }

        Map<String, OrderItem> currentByMarketplaceId = new LinkedHashMap<>();
        for (OrderItem currentItem : currentItems) {
            currentByMarketplaceId.put(currentItem.getIdItemMarketplace(), currentItem);
        }

        for (OrderItem desiredItem : desiredItems) {
            OrderItem currentItem = currentByMarketplaceId.get(desiredItem.getIdItemMarketplace());
            if (currentItem == null) {
                return false;
            }
            Integer currentProductId = currentItem.getId() == null ? null : currentItem.getId().getIdProduto();
            Integer desiredProductId = desiredItem.getId() == null ? null : desiredItem.getId().getIdProduto();
            if (!java.util.Objects.equals(currentProductId, desiredProductId)) {
                return false;
            }
            if (!java.util.Objects.equals(currentItem.getQuantidade(), desiredItem.getQuantidade())) {
                return false;
            }
            if (!java.util.Objects.equals(currentItem.getPrecoUnitario(), desiredItem.getPrecoUnitario())) {
                return false;
            }
        }

        return true;
    }

    private void reconcileMarketplaceOrderInventory(Order order, List<OrderItem> orderItems, boolean inventoryWasApplied, Integer usuarioId) {
        boolean shouldApplyInventory = !"CANCELADO".equalsIgnoreCase(order.getStatusPedido());

        if (shouldApplyInventory && !inventoryWasApplied) {
            applyInventoryAdjustment(orderItems, -1, usuarioId);
            order.setInventoryApplied(Boolean.TRUE);
            orderRepository.save(order);
            return;
        }

        if (!shouldApplyInventory && inventoryWasApplied) {
            applyInventoryAdjustment(orderItems, 1, usuarioId);
            order.setInventoryApplied(Boolean.FALSE);
            orderRepository.save(order);
        }
    }

    private void applyInventoryAdjustment(List<OrderItem> orderItems, int direction, Integer usuarioId) {
        for (OrderItem orderItem : orderItems) {
            Integer productId = orderItem.getId() == null ? null : orderItem.getId().getIdProduto();
            if (productId == null) {
                continue;
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Produto vinculado ao pedido do marketplace não encontrado."));

            if (usuarioId != null && !usuarioId.equals(product.getUsuario())) {
                throw new RuntimeException("Produto vinculado ao pedido do marketplace não pertence ao usuário esperado.");
            }

            int currentQuantity = product.getQuantidade() == null ? 0 : product.getQuantidade();
            int amount = orderItem.getQuantidade() == null ? 0 : orderItem.getQuantidade();
            if (amount <= 0) {
                continue;
            }

            int nextQuantity = adjustInventoryAtomically(product, direction, amount, currentQuantity);

            product.setQuantidade(nextQuantity);
        }
    }

    private int adjustInventoryAtomically(Product product, int direction, int amount, int currentQuantity) {
        if (direction < 0) {
            int updatedRows = productRepository.decrementQuantityIfEnough(product.getIdProduto(), amount);
            if (updatedRows == 0 && isListingInventoryShadow(product)) {
                productRepository.incrementQuantity(product.getIdProduto(), amount);
                updatedRows = productRepository.decrementQuantityIfEnough(product.getIdProduto(), amount);
            }
            if (updatedRows == 0) {
                throw new RuntimeException("Estoque insuficiente para sincronizar pedido do marketplace.");
            }
        } else if (direction > 0) {
            productRepository.incrementQuantity(product.getIdProduto(), amount);
        } else {
            return currentQuantity;
        }

        return productRepository.findById(product.getIdProduto())
                .map(existing -> existing.getQuantidade() == null ? 0 : existing.getQuantidade())
                .orElseThrow(() -> new RuntimeException("Produto vinculado ao pedido do marketplace não encontrado."));
    }

    private BigDecimal convertToBigDecimal(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (rawValue instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(String.valueOf(rawValue)).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private UserMarketplace getConnectionOrThrow(Integer usuarioId) {
        return userMarketplaceRepository.findFirstByUsuarioAndMarketplaceId(usuarioId, 1)
                .orElseThrow(() -> new RuntimeException("Mercado Livre não conectado."));
    }

    private UserMarketplace ensureValidAccessToken(UserMarketplace userMarketplace) {
        if (userMarketplace.getTokenExpiration() != null
                && userMarketplace.getTokenExpiration().isAfter(LocalDateTime.now().plusMinutes(1))) {
            return userMarketplace;
        }

        return refreshAccessToken(userMarketplace);
    }

    private UserMarketplace refreshAccessToken(UserMarketplace userMarketplace) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", userMarketplace.getRefreshToken());

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = postForEntityWithRetry(
                    "oauth-refresh-token",
                    "https://api.mercadolibre.com/oauth/token",
                    request,
                    Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Resposta inválida ao renovar token do Mercado Livre.");
            }

            Map<String, Object> data = response.getBody();
            Number expiresIn = (Number) data.get("expires_in");
            userMarketplace.setAccessToken((String) data.get("access_token"));
            userMarketplace.setRefreshToken((String) data.get("refresh_token"));
            userMarketplace.setTokenExpiration(LocalDateTime.now().plusSeconds(expiresIn.longValue()));
            userMarketplace.setStatusIntegracao("CONECTADO");

            return userMarketplaceRepository.save(userMarketplace);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            throw new RuntimeException("Falha ao renovar token do Mercado Livre: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao renovar token do Mercado Livre: " + e.getMessage(), e);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return String.valueOf(payload);
        }
    }

    private int fetchTotalInventory(UserMarketplace connection) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = exchangeWithRetry(
                "https://api.mercadolibre.com/users/" + connection.getIdLoja() + "/items/search?limit=1",
                HttpMethod.GET,
                request,
            Map.class,
            "inventory-total");

        return extractPagingTotal(response.getBody());
    }

    private int fetchActiveListings(UserMarketplace connection) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = exchangeWithRetry(
                "https://api.mercadolibre.com/users/" + connection.getIdLoja() + "/items/search?status=active&limit=1",
                HttpMethod.GET,
                request,
            Map.class,
            "active-listings");

        return extractPagingTotal(response.getBody());
    }

    private int fetchSellerCompletedSales(UserMarketplace connection) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = exchangeWithRetry(
                "https://api.mercadolibre.com/users/" + connection.getIdLoja(),
                HttpMethod.GET,
                request,
            Map.class,
            "seller-profile");

        return extractCompletedSales(response.getBody());
    }

    private int countSyncedInventoryQuantity(Integer usuarioId) {
        return marketplaceListingRepository.findByUsuarioAndMarketplaceSource(usuarioId, MERCADO_LIVRE_SOURCE)
                .stream()
                .map(MarketplaceListing::getQuantidadeDisponivel)
                .filter(quantity -> quantity != null && quantity > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }
    private int fetchTotalOrders(UserMarketplace connection, boolean onlyActive) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        String fromDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC).minusMonths(12));
        String toDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC));

        StringBuilder url = new StringBuilder("https://api.mercadolibre.com/orders/search?seller=")
                .append(connection.getIdLoja())
                .append("&order.date_created.from=")
                .append(encode(fromDate))
                .append("&order.date_created.to=")
                .append(encode(toDate))
                .append("&limit=1");

        if (onlyActive) {
            url.append("&order.status=")
                .append(ACTIVE_ORDER_STATUS_FILTER);
        }

        ResponseEntity<Map> response = exchangeWithRetry(url.toString(), HttpMethod.GET, request, Map.class,
            "orders-total");
        return extractPagingTotal(response.getBody());
    }

    private List<Map<String, Object>> fetchOrders(UserMarketplace connection, String orderStatusFilter) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        String fromDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC).minusMonths(12));
        String toDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC));

        List<Map<String, Object>> results = new ArrayList<>();
        int offset = 0;
        int limit = 50;
        int total;

        do {
            StringBuilder url = new StringBuilder("https://api.mercadolibre.com/orders/search?seller=")
                    .append(connection.getIdLoja())
                    .append("&order.date_created.from=")
                    .append(encode(fromDate))
                    .append("&order.date_created.to=")
                    .append(encode(toDate))
                    .append("&sort=date_desc")
                    .append("&limit=")
                    .append(limit)
                    .append("&offset=")
                    .append(offset);

            if (orderStatusFilter != null && !orderStatusFilter.isBlank()) {
                url.append("&order.status=").append(orderStatusFilter);
            }

                ResponseEntity<Map> response = exchangeWithRetry(url.toString(), HttpMethod.GET, request, Map.class,
                    "orders-search");
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> batch = body == null ? List.of() : (List<Map<String, Object>>) body.getOrDefault("results", List.of());
            results.addAll(batch);

            total = extractPagingTotal(body);
            offset += limit;
        } while (offset < total);

        return results;
    }

    private Map<String, Object> fetchOrderDetails(UserMarketplace connection, String orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = exchangeWithRetry(
                "https://api.mercadolibre.com/orders/" + orderId,
                HttpMethod.GET,
                request,
            Map.class,
            "order-details");

        Map<String, Object> body = response.getBody();
        return body == null ? Map.of("id", orderId) : body;
    }

    private int extractPagingTotal(Map body) {
        if (body == null) {
            return 0;
        }

        Object pagingObj = body.get("paging");
        if (!(pagingObj instanceof Map<?, ?> paging)) {
            return 0;
        }

        Object totalObj = paging.get("total");
        if (totalObj instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    private int extractCompletedSales(Map body) {
        if (body == null) {
            return 0;
        }

        Object sellerReputationObj = body.get("seller_reputation");
        if (!(sellerReputationObj instanceof Map<?, ?> sellerReputation)) {
            return 0;
        }

        Object transactionsObj = sellerReputation.get("transactions");
        if (!(transactionsObj instanceof Map<?, ?> transactions)) {
            return 0;
        }

        Object completedObj = transactions.get("completed");
        if (completedObj instanceof Number completedNumber) {
            return completedNumber.intValue();
        }

        Object totalObj = transactions.get("total");
        if (totalObj instanceof Number totalNumber) {
            return totalNumber.intValue();
        }

        return 0;
    }
    private int safeCount(CountSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private <T> ResponseEntity<T> exchangeWithRetry(String url, HttpMethod method, HttpEntity<?> request,
            Class<T> responseType, String operation) {
        return executeWithRetry(operation, () -> restTemplate.exchange(url, method, request, responseType));
    }

    private <T> ResponseEntity<T> postForEntityWithRetry(String operation, String url, HttpEntity<?> request,
            Class<T> responseType) {
        return executeWithRetry(operation, () -> restTemplate.postForEntity(url, request, responseType));
    }

    private <T> ResponseEntity<T> executeWithRetry(String operation, Supplier<ResponseEntity<T>> supplier) {
        int maxAttempts = Math.max(retryMaxAttempts, 1);
        long backoffMs = Math.max(retryInitialBackoffMs, 0L);
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                awaitMarketplaceRateLimitSlot();
                return supplier.get();
            } catch (HttpClientErrorException exception) {
                if (!shouldRetry(exception.getStatusCode())) {
                    throw exception;
                }
                lastFailure = exception;
            } catch (HttpServerErrorException | ResourceAccessException exception) {
                lastFailure = exception;
            }

            if (attempt == maxAttempts) {
                break;
            }

            sleepBackoff(backoffMs);
            backoffMs = nextBackoff(backoffMs);
        }

        throw new RuntimeException("Falha ao executar operação Mercado Livre após retry: " + operation,
                lastFailure);
    }

    private boolean shouldRetry(HttpStatusCode statusCode) {
        if (statusCode == null) {
            return false;
        }

        return statusCode.value() == 408 || statusCode.value() == 429 || statusCode.is5xxServerError();
    }

    private long nextBackoff(long currentBackoffMs) {
        if (currentBackoffMs <= 0) {
            return 500L;
        }
        return Math.min(currentBackoffMs * 2L, 8000L);
    }

    private void sleepBackoff(long backoffMs) {
        if (backoffMs <= 0) {
            return;
        }

        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrompido durante backoff do Mercado Livre.", exception);
        }
    }

    private void awaitMarketplaceRateLimitSlot() {
        long minIntervalMs = Math.max(rateLimitMinIntervalMs, 0L);
        if (minIntervalMs == 0L) {
            return;
        }

        while (true) {
            long waitMs;
            synchronized (rateLimitMonitor) {
                long now = System.currentTimeMillis();
                if (now >= nextMarketplaceRequestAtMs) {
                    nextMarketplaceRequestAtMs = now + minIntervalMs;
                    return;
                }
                waitMs = nextMarketplaceRequestAtMs - now;
            }

            sleepBackoff(waitMs);
        }
    }

    @FunctionalInterface
    private interface CountSupplier {
        int get();
    }

    private LocalDate parseMercadoLivreDate(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(String.valueOf(rawValue)).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String mapMercadoLivreOrderStatus(Map<String, Object> orderMap) {
        List<String> tags = extractStringList(orderMap.get("tags"));
        String orderStatus = orderMap.get("status") == null ? "" : String.valueOf(orderMap.get("status")).toLowerCase();

        if ("cancelled".equals(orderStatus) || "invalid".equals(orderStatus)) {
            return "CANCELADO";
        }

        if (tags.contains("delivered")) {
            return "CONCLUÍDO";
        }

        return "EM ANDAMENTO";
    }

    private String mapMercadoLivrePaymentStatus(Map<String, Object> orderMap) {
        Object paymentsObj = orderMap.get("payments");
        if (paymentsObj instanceof List<?> payments && !payments.isEmpty() && payments.get(0) instanceof Map<?, ?> paymentMap) {
            Object paymentStatusRaw = paymentMap.get("status");
            Object paymentDetailRaw = paymentMap.get("status_detail");
            String paymentStatus = paymentStatusRaw == null ? "" : String.valueOf(paymentStatusRaw).toLowerCase();
            String paymentDetail = paymentDetailRaw == null ? "" : String.valueOf(paymentDetailRaw).toLowerCase();

            if (paymentStatus.equals("approved") || paymentDetail.equals("accredited")) {
                return "PROCESSADO";
            }
            if (paymentStatus.equals("rejected")) {
                return "NEGADO";
            }
            if (paymentStatus.equals("cancelled") || paymentStatus.equals("refunded") || paymentStatus.equals("charged_back")) {
                return "CANCELADO";
            }
        }

        String orderStatus = orderMap.get("status") == null ? "" : String.valueOf(orderMap.get("status")).toLowerCase();
        if ("cancelled".equals(orderStatus) || "invalid".equals(orderStatus)) {
            return "CANCELADO";
        }

        return "EM PROCESSAMENTO";
    }

    private String resolveNormalizedBrand(Map<String, Object> itemMap, String title) {
        String brand = extractBrandFromAttributes(itemMap.get("attributes"));
        if (isMeaningfulMarketplaceValue(brand)) {
            return normalizeBrandName(brand);
        }

        Object variationsObj = itemMap.get("variations");
        if (variationsObj instanceof List<?> variations) {
            for (Object variationObj : variations) {
                if (!(variationObj instanceof Map<?, ?> variationMap)) {
                    continue;
                }

                brand = extractBrandFromAttributes(variationMap.get("attributes"));
                if (isMeaningfulMarketplaceValue(brand)) {
                    return normalizeBrandName(brand);
                }
            }
        }

        brand = inferBrandFromTitle(title);
        if (isMeaningfulMarketplaceValue(brand)) {
            return brand;
        }

        return "N/A";
    }

    private String extractBrandFromAttributes(Object attributesObj) {
        if (!(attributesObj instanceof List<?> attributes)) {
            return "";
        }

        for (Object attributeObj : attributes) {
            if (!(attributeObj instanceof Map<?, ?> attributeMap)) {
                continue;
            }

            Object attributeId = attributeMap.get("id");
            if (attributeId == null) {
                continue;
            }

            String normalizedAttributeId = String.valueOf(attributeId).toUpperCase();
            if (!"BRAND".equals(normalizedAttributeId) && !"MANUFACTURER".equals(normalizedAttributeId)) {
                continue;
            }

            Object valueName = attributeMap.get("value_name");
            if (valueName != null && !String.valueOf(valueName).isBlank()) {
                return String.valueOf(valueName).trim();
            }
        }

        return "";
    }

    private String inferBrandFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return "N/A";
        }

        String normalizedTitle = normalizeText(title);
        for (Map.Entry<String, String> entry : NORMALIZED_BRAND_BY_TOKEN.entrySet()) {
            if (containsWholeToken(normalizedTitle, entry.getKey())) {
                return entry.getValue();
            }
        }

        return "N/A";
    }

    private String normalizeBrandName(String rawValue) {
        if (!isMeaningfulMarketplaceValue(rawValue)) {
            return "N/A";
        }

        String normalized = normalizeText(rawValue);
        for (Map.Entry<String, String> entry : NORMALIZED_BRAND_BY_TOKEN.entrySet()) {
            if (normalized.equals(entry.getKey()) || containsWholeToken(normalized, entry.getKey())) {
                return entry.getValue();
            }
        }

        return rawValue.trim();
    }

    private boolean containsWholeToken(String normalizedValue, String normalizedCandidate) {
        if (normalizedValue == null || normalizedValue.isBlank() || normalizedCandidate == null || normalizedCandidate.isBlank()) {
            return false;
        }

        return (" " + normalizedValue + " ").contains(" " + normalizedCandidate + " ");
    }

    private boolean isMeaningfulMarketplaceValue(String value) {
        return value != null && !value.isBlank() && !"N/A".equalsIgnoreCase(value.trim());
    }

    private String extractBarcode(Map<String, Object> itemMap) {
        String barcode = extractBarcodeFromAttributes(itemMap.get("attributes"));
        if (!barcode.isBlank()) {
            return barcode;
        }

        Object variationsObj = itemMap.get("variations");
        if (variationsObj instanceof List<?> variations) {
            for (Object variationObj : variations) {
                if (!(variationObj instanceof Map<?, ?> variationMap)) {
                    continue;
                }

                barcode = extractBarcodeFromAttributes(variationMap.get("attributes"));
                if (!barcode.isBlank()) {
                    return barcode;
                }
            }
        }

        return "N/A";
    }

    private String extractBarcodeFromAttributes(Object attributesObj) {
        if (!(attributesObj instanceof List<?> attributes)) {
            return "";
        }

        for (Object attributeObj : attributes) {
            if (!(attributeObj instanceof Map<?, ?> attributeMap)) {
                continue;
            }

            Object attributeId = attributeMap.get("id");
            if (attributeId == null) {
                continue;
            }

            String normalizedAttributeId = String.valueOf(attributeId).toUpperCase();
            if (!"EAN".equals(normalizedAttributeId)
                    && !"GTIN".equals(normalizedAttributeId)
                    && !"UPC".equals(normalizedAttributeId)
                    && !"ISBN".equals(normalizedAttributeId)) {
                continue;
            }

            Object valueName = attributeMap.get("value_name");
            if (valueName != null && !String.valueOf(valueName).isBlank()) {
                return String.valueOf(valueName).trim();
            }

            Object valueId = attributeMap.get("value_id");
            if (valueId != null && !String.valueOf(valueId).isBlank()) {
                return String.valueOf(valueId).trim();
            }
        }

        return "";
    }

    private String resolveNormalizedCategory(Map<String, Object> itemMap, String title) {
        String categoryId = itemMap.get("category_id") == null ? null : String.valueOf(itemMap.get("category_id"));
        String marketplaceCategoryName = fetchMarketplaceCategoryName(categoryId);
        String normalizedFromCategory = normalizeCategoryName(marketplaceCategoryName);
        if (normalizedFromCategory != null) {
            return normalizedFromCategory;
        }

        String normalizedFromTitle = normalizeCategoryName(title);
        if (normalizedFromTitle != null) {
            return normalizedFromTitle;
        }

        return "Outros";
    }

    private String fetchMarketplaceCategoryName(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }

        return categoryNameCache.computeIfAbsent(categoryId, this::requestCategoryName);
    }

    private String requestCategoryName(String categoryId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.mercadolibre.com/categories/" + encode(categoryId),
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    Map.class);

            Map<String, Object> body = response.getBody();
            Object name = body == null ? null : body.get("name");
            if (name == null) {
                return "";
            }

            String categoryName = String.valueOf(name).trim();
            return categoryName.isEmpty() ? "" : categoryName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeCategoryName(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = normalizeText(rawValue);

        if (containsAny(normalized, "placa mae", "motherboard")) {
            return "Placas-mãe";
        }
        if (containsAny(normalized, "placa de video", "placa video", "gpu", "video card", "placa grafica")) {
            return "Placas de vídeo";
        }
        if (containsAny(normalized, "processador", "cpu")) {
            return "Processadores";
        }
        if (containsAny(normalized, "memoria ram", "memoria ddr", "ram")) {
            return "Memórias RAM";
        }
        if (containsAny(normalized, "ssd", "hd", "hdd", "armazenamento", "disco rigido", "nvme", "m.2", "cartao de memoria", "pendrive")) {
            return "Armazenamento";
        }
        if (containsAny(normalized, "fonte", "power supply", "psu")) {
            return "Fontes";
        }
        if (containsAny(normalized, "cooler", "water cooler", "air cooler", "fan")) {
            return "Coolers";
        }

        return null;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase();
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String buildMarketplaceOrderObservation(Map<String, Object> orderMap, String resourceId) {
        String marketplaceNumber = extractMarketplaceDisplayNumber(orderMap, resourceId);
        return marketplaceNumber == null ? null : "Número - " + marketplaceNumber;
    }

    private String extractMarketplaceDisplayNumber(Map<String, Object> orderMap, String resourceId) {
        Object packId = orderMap.get("pack_id");
        if (packId != null && !String.valueOf(packId).isBlank() && !"null".equalsIgnoreCase(String.valueOf(packId))) {
            return String.valueOf(packId);
        }
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }
        return resourceId;
    }

    private List<String> extractStringList(Object rawList) {
        if (!(rawList instanceof List<?> list)) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                values.add(String.valueOf(item).toLowerCase());
            }
        }
        return values;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private <T> List<List<T>> partition(List<T> source, int size) {
        List<List<T>> batches = new ArrayList<>();
        for (int index = 0; index < source.size(); index += size) {
            batches.add(source.subList(index, Math.min(index + size, source.size())));
        }
        return batches;
    }

    private String buildOauthState(Integer usuarioId) {
        return usuarioId + ":" + generateStateToken();
    }

    private String generateStateToken() {
        byte[] buffer = new byte[18];
        SECURE_RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String generateCodeVerifier() {
        byte[] buffer = new byte[64];
        SECURE_RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Falha ao gerar code_challenge PKCE.", e);
        }
    }
}
