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
        return ResponseEntity.ok(Map.of("url", mercadoLivreService.getAuthorizationUrl()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus(@RequestParam(defaultValue = "1") Integer usuarioId) {
        boolean isConnected = mercadoLivreService.isConnected(usuarioId);
        return ResponseEntity.ok(Map.of("connected", isConnected));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code,
            @RequestParam(value = "state", defaultValue = "1") String state) {
        try {
            Integer usuarioId = Integer.parseInt(state);

            mercadoLivreService.processCallback(code, usuarioId);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://localhost:5173/dashboard/conexoes");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("FALHA AO PROCESSAR CALLBACK: " + e.getMessage());
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncInventory(@RequestParam(defaultValue = "1") Integer usuarioId) {
        try {
            mercadoLivreService.syncInventory(usuarioId);
            return ResponseEntity.ok(Map.of("message", "Inventário sincronizado com o banco local."));
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            return ResponseEntity.status(500).body(Map.of("message", "ERRO HTTP DA API DO ML: " + e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "ERRO INTERNO NO SYNC: " + e.getMessage()));
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(@RequestParam(defaultValue = "1") Integer usuarioId) {
        try {
            mercadoLivreService.disconnect(usuarioId);
            return ResponseEntity.ok(Map.of("message", "Desconectado com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "FALHA INTERNA DESCONECTAR: " + e.getMessage()));
        }
    }
}
