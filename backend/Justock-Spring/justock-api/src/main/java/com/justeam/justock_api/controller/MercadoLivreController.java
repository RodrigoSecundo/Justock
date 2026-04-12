package com.justeam.justock_api.controller;

import com.justeam.justock_api.service.MercadoLivreService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mercadolivre")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MercadoLivreController {

    private final MercadoLivreService mercadoLivreService;

    public MercadoLivreController(MercadoLivreService mercadoLivreService) {
        this.mercadoLivreService = mercadoLivreService;
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        Integer usuarioId = mercadoLivreService.getIntegrationUserId();
        return ResponseEntity.ok(Map.of("url", mercadoLivreService.getAuthorizationUrl(usuarioId)));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Integer usuarioId = mercadoLivreService.getIntegrationUserId();
        return ResponseEntity.ok(mercadoLivreService.getConnectionSummary(usuarioId));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code,
            @RequestParam("state") String state) {
        try {
            mercadoLivreService.processCallback(code, state);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", mercadoLivreService.getFrontendRedirectUri());
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("FALHA AO PROCESSAR CALLBACK: " + e.getMessage());
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncInventory() {
        try {
            Integer usuarioId = mercadoLivreService.getIntegrationUserId();
            Map<String, Object> summary = mercadoLivreService.syncMarketplaceData(usuarioId);
            return ResponseEntity.ok(Map.of(
                    "message", "Pedidos e produtos sincronizados com o banco local.",
                    "summary", summary));
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            return ResponseEntity.status(500).body(Map.of("message", "ERRO HTTP DA API DO ML: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "ERRO INTERNO NO SYNC: " + e.getMessage()));
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect() {
        try {
            Integer usuarioId = mercadoLivreService.getIntegrationUserId();
            mercadoLivreService.disconnect(usuarioId);
            return ResponseEntity.ok(Map.of("message", "Desconectado com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "FALHA INTERNA DESCONECTAR: " + e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody Map<String, Object> payload) {
        mercadoLivreService.registerWebhookEvent(payload);
        return ResponseEntity.ok(Map.of("message", "Webhook recebido com sucesso."));
    }
}
