package com.justeam.justock_api.service;

import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.model.UserMarketplace;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.repository.UserMarketplaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MercadoLivreService {

    @Value("${mercadolivre.client.id}")
    private String clientId;

    @Value("${mercadolivre.client.secret}")
    private String clientSecret;

    @Value("${mercadolivre.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final UserMarketplaceRepository userMarketplaceRepository;
    private final ProductRepository productRepository;

    public MercadoLivreService(RestTemplate restTemplate, UserMarketplaceRepository userMarketplaceRepository,
            ProductRepository productRepository) {
        this.restTemplate = restTemplate;
        this.userMarketplaceRepository = userMarketplaceRepository;
        this.productRepository = productRepository;
    }

    public String getAuthorizationUrl() {
        try {
            String encodedUri = java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8.name());
            return "https://auth.mercadolivre.com.br/authorization?response_type=code&client_id=" + clientId
                    + "&redirect_uri=" + encodedUri;
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isConnected(Integer usuarioId) {
        return !userMarketplaceRepository.findByUsuario(usuarioId).isEmpty();
    }

    public void processCallback(String code, Integer usuarioId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity("https://api.mercadolibre.com/oauth/token",
                    request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = response.getBody();
                String accessToken = (String) data.get("access_token");
                String refreshToken = (String) data.get("refresh_token");
                Integer expiresIn = (Integer) data.get("expires_in");
                String mlUserId = String.valueOf(data.get("user_id"));

                List<UserMarketplace> list = userMarketplaceRepository.findByUsuario(usuarioId);
                UserMarketplace um = list.isEmpty() ? new UserMarketplace() : list.get(0);
                um.setUsuario(usuarioId);
                um.setMarketplaceId(1);
                um.setAccessToken(accessToken);
                um.setRefreshToken(refreshToken);
                um.setTokenExpiration(LocalDateTime.now().plusSeconds(expiresIn));
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

    public void syncInventory(Integer usuarioId) {
        List<UserMarketplace> list = userMarketplaceRepository.findByUsuario(usuarioId);
        if (list.isEmpty()) {
            throw new RuntimeException("Mercado Livre não conectado.");
        }
        UserMarketplace um = list.get(0);

        if (um.getTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado. Por favor reconecte.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(um.getAccessToken());
        HttpEntity<String> request = new HttpEntity<>(headers);

        String urlIds = "https://api.mercadolibre.com/users/" + um.getIdLoja() + "/items/search";
        ResponseEntity<Map> searchResponse = restTemplate.exchange(urlIds, HttpMethod.GET, request, Map.class);

        if (searchResponse.getStatusCode() == HttpStatus.OK && searchResponse.getBody() != null) {
            List<String> itemsId = (List<String>) searchResponse.getBody().get("results");

            if (itemsId != null && !itemsId.isEmpty()) {
                String idsParam = String.join(",", itemsId);
                String urlItems = "https://api.mercadolibre.com/items?ids=" + idsParam;
                ResponseEntity<List> itemsResponse = restTemplate.exchange(urlItems, HttpMethod.GET, request,
                        List.class);

                if (itemsResponse.getStatusCode() == HttpStatus.OK && itemsResponse.getBody() != null) {
                    List<Map<String, Object>> itemsList = itemsResponse.getBody();
                    for (Map<String, Object> itemWrapper : itemsList) {
                        if (itemWrapper.get("code").equals(200)) {
                            Map<String, Object> itemMap = (Map<String, Object>) itemWrapper.get("body");
                            saveOrUpdateProduct(itemMap, usuarioId);
                        }
                    }
                }
            }
        }
    }

    private void saveOrUpdateProduct(Map<String, Object> itemMap, Integer usuarioId) {
        String mlId = (String) itemMap.get("id");
        String title = (String) itemMap.get("title");
        Number priceNum = (Number) itemMap.get("price");
        Number availableQty = (Number) itemMap.get("available_quantity");

        java.math.BigDecimal price = new java.math.BigDecimal(priceNum.toString());
        Integer quantity = availableQty.intValue();

        Product product = new Product();
        product.setCategoria("Mercado Livre");
        product.setMarca("N/A");
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

    @org.springframework.transaction.annotation.Transactional
    public void disconnect(Integer usuarioId) {
        List<UserMarketplace> list = userMarketplaceRepository.findByUsuario(usuarioId);
        userMarketplaceRepository.deleteAll(list);
    }
}
