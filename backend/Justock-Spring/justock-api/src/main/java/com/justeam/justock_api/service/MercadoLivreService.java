package com.justeam.justock_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.justock_api.model.Order;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.model.UserMarketplace;
import com.justeam.justock_api.model.WebhookEvent;
import com.justeam.justock_api.repository.OrderRepository;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.repository.UserMarketplaceRepository;
import com.justeam.justock_api.repository.WebhookEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MercadoLivreService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ACTIVE_ORDER_STATUS_FILTER = String.join(",",
            "confirmed",
            "payment_required",
            "payment_in_process",
            "partially_paid",
            "paid",
            "partially_refunded");

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

    private final RestTemplate restTemplate;
    private final UserMarketplaceRepository userMarketplaceRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, String> pkceVerifierByState = new ConcurrentHashMap<>();

    public MercadoLivreService(RestTemplate restTemplate, UserMarketplaceRepository userMarketplaceRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            WebhookEventRepository webhookEventRepository,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.userMarketplaceRepository = userMarketplaceRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.objectMapper = objectMapper;
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

        try {
            summary.put("totalVendas", fetchTotalOrders(connection, false));
            summary.put("pedidosAtivos", fetchTotalOrders(connection, true));
            summary.put("totalInventario", fetchTotalInventory(connection));
        } catch (Exception ignored) {
            summary.put("totalVendas", 0);
            summary.put("pedidosAtivos", 0);
            summary.put("totalInventario", 0);
        }

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

            ResponseEntity<Map> response = restTemplate.postForEntity("https://api.mercadolibre.com/oauth/token",
                    request, Map.class);

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
        syncInventory(usuarioId);
        syncOrders(usuarioId);
        return getConnectionSummary(usuarioId);
    }

    public void syncInventory(Integer usuarioId) {
        UserMarketplace um = ensureValidAccessToken(getConnectionOrThrow(usuarioId));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(um.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        String urlIds = "https://api.mercadolibre.com/users/" + um.getIdLoja() + "/items/search";
        ResponseEntity<Map> searchResponse = restTemplate.exchange(urlIds, HttpMethod.GET, request, Map.class);

        if (searchResponse.getStatusCode().is2xxSuccessful() && searchResponse.getBody() != null) {
            List<String> itemsId = (List<String>) searchResponse.getBody().get("results");
            Set<String> syncedItemIds = new HashSet<>();

            if (itemsId != null && !itemsId.isEmpty()) {
                syncedItemIds.addAll(itemsId);
                for (List<String> chunk : partition(itemsId, 20)) {
                    String idsParam = String.join(",", chunk);
                    String urlItems = "https://api.mercadolibre.com/items?ids=" + idsParam;
                    ResponseEntity<List> itemsResponse = restTemplate.exchange(urlItems, HttpMethod.GET, request,
                            List.class);

                    if (itemsResponse.getStatusCode().is2xxSuccessful() && itemsResponse.getBody() != null) {
                        List<Map<String, Object>> itemsList = itemsResponse.getBody();
                        for (Map<String, Object> itemWrapper : itemsList) {
                            if (Integer.valueOf(200).equals(itemWrapper.get("code"))) {
                                Map<String, Object> itemMap = (Map<String, Object>) itemWrapper.get("body");
                                saveOrUpdateProduct(itemMap, usuarioId);
                            }
                        }
                    }
                }
            }

            removeProductsMissingFromLatestSync(usuarioId, syncedItemIds);
        }
    }

    public void syncOrders(Integer usuarioId) {
        UserMarketplace connection = ensureValidAccessToken(getConnectionOrThrow(usuarioId));
        List<Map<String, Object>> orders = fetchOrders(connection, null);
        Set<String> syncedOrderIds = new HashSet<>();

        for (Map<String, Object> orderMap : orders) {
            Object orderId = orderMap.get("id");
            if (orderId == null) {
                continue;
            }

            String resourceId = String.valueOf(orderId);
            syncedOrderIds.add(resourceId);
            saveOrUpdateOrder(orderMap, connection, resourceId);
        }

        removeOrdersMissingFromLatestSync(syncedOrderIds);
    }

    private void saveOrUpdateProduct(Map<String, Object> itemMap, Integer usuarioId) {
        String mlId = (String) itemMap.get("id");
        String title = (String) itemMap.get("title");
        Number priceNum = (Number) itemMap.get("price");
        Number availableQty = (Number) itemMap.get("available_quantity");

        BigDecimal price = new BigDecimal(priceNum.toString());
        Integer quantity = availableQty.intValue();
        String brand = extractBrand(itemMap);

        Product product = productRepository.findByMarketplaceResourceIdAndUsuario(mlId, usuarioId)
            .orElseGet(Product::new);
        product.setCategoria("Mercado Livre");
        product.setMarca(brand);
        product.setNomeDoProduto(title);
        product.setEstado("Ativo");
        product.setPreco(price);
        product.setCodigoDeBarras(mlId);
        product.setQuantidade(quantity);
        product.setQuantidadeReservada(0);
        product.setMarcador("ML");
        product.setUsuario(usuarioId);
        product.setMarketplaceResourceId(mlId);
        product.setMarketplaceSource("MERCADO_LIVRE");

        productRepository.save(product);
    }

    private void saveOrUpdateOrder(Map<String, Object> orderMap, UserMarketplace connection, String resourceId) {
        Order order = orderRepository.findByMarketplaceResourceIdAndMarketplaceSource(resourceId, "MERCADO_LIVRE")
                .orElseGet(Order::new);

        order.setIdPedidoMarketplace(1);
        order.setUsuarioMarketplaceId(connection.getUsuarioMarketplaceId());
        order.setDataEmissao(parseMercadoLivreDate(orderMap.get("date_created")));
        order.setDataEntrega(null);
        order.setStatusPedido(mapMercadoLivreOrderStatus(orderMap));
        order.setStatusPagamento(mapMercadoLivrePaymentStatus(orderMap));
        order.setMarketplaceSource("MERCADO_LIVRE");
        order.setMarketplaceResourceId(resourceId);

        orderRepository.save(order);
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
        webhookEvent.setError(null);

        webhookEventRepository.save(webhookEvent);
    }

    @org.springframework.transaction.annotation.Transactional
    public void disconnect(Integer usuarioId) {
        List<UserMarketplace> list = userMarketplaceRepository.findByUsuario(usuarioId);
        userMarketplaceRepository.deleteAll(list);
    }

    private void removeProductsMissingFromLatestSync(Integer usuarioId, Set<String> syncedItemIds) {
        List<Product> existingProducts = productRepository.findByUsuarioAndMarketplaceSource(usuarioId, "MERCADO_LIVRE");

        for (Product product : existingProducts) {
            String resourceId = product.getMarketplaceResourceId();
            if (resourceId == null || !syncedItemIds.contains(resourceId)) {
                productRepository.delete(product);
            }
        }
    }

    private void removeOrdersMissingFromLatestSync(Set<String> syncedOrderIds) {
        List<Order> existingOrders = orderRepository.findByMarketplaceSource("MERCADO_LIVRE");

        for (Order order : existingOrders) {
            String resourceId = order.getMarketplaceResourceId();
            if (resourceId == null || !syncedOrderIds.contains(resourceId)) {
                orderRepository.delete(order);
            }
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
            ResponseEntity<Map> response = restTemplate.postForEntity("https://api.mercadolibre.com/oauth/token",
                    request, Map.class);

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

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.mercadolibre.com/users/" + connection.getIdLoja() + "/items/search?limit=1",
                HttpMethod.GET,
                request,
                Map.class);

        return extractPagingTotal(response.getBody());
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

        ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.GET, request, Map.class);
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

            ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.GET, request, Map.class);
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> batch = body == null ? List.of() : (List<Map<String, Object>>) body.getOrDefault("results", List.of());
            results.addAll(batch);

            total = extractPagingTotal(body);
            offset += limit;
        } while (offset < total);

        return results;
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

    private String extractBrand(Map<String, Object> itemMap) {
        Object attributesObj = itemMap.get("attributes");
        if (attributesObj instanceof List<?> attributes) {
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
                    return String.valueOf(valueName);
                }
            }
        }

        return "N/A";
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
