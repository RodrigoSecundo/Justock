package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.WebhookEventResponseDTO;
import com.justeam.justock_api.model.WebhookEvent;
import com.justeam.justock_api.request.WebhookEventCreateRequest;
import com.justeam.justock_api.request.WebhookEventUpdateRequest;
import com.justeam.justock_api.service.WebhookEventService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/WebhookEvent")
@CrossOrigin(origins = "*")
public class WebhookEventController {

    @Autowired
    private WebhookEventService webhookEventService;

    // GET /api/WebhookEvent
    @GetMapping("/")
    public ApiResponseDTO<List<WebhookEventResponseDTO>> index() {
        List<WebhookEventResponseDTO> webhookEvents = webhookEventService.listAllWebhookEvents()
                .stream()
                .map(we -> new WebhookEventResponseDTO(we.getId(), we.getUsuarioMarketplaceId(), we.getMarketplaceId(), we.getTipoEvento(), we.getPayload(), we.getReceivedAt(), we.getProcessed(), we.getProcessedAt(), we.getError()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "WebhookEvents encontrados!", webhookEvents);
    }

    // GET /api/WebhookEvent/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    public ApiResponseDTO<WebhookEventResponseDTO> show(@PathVariable int id) {
        WebhookEvent webhookEvent = webhookEventService.findWebhookEvent(id);
        if (webhookEvent == null) {
            return new ApiResponseDTO<>(404, "WebhookEvent não encontrado!", null);
        }
        WebhookEventResponseDTO dto = new WebhookEventResponseDTO(webhookEvent.getId(), webhookEvent.getUsuarioMarketplaceId(), webhookEvent.getMarketplaceId(), webhookEvent.getTipoEvento(), webhookEvent.getPayload(), webhookEvent.getReceivedAt(), webhookEvent.getProcessed(), webhookEvent.getProcessedAt(), webhookEvent.getError());
        return new ApiResponseDTO<>(200, "WebhookEvent encontrado!", dto);
    }

    // POST /api/WebhookEvent/cadastrar
    @PostMapping("/cadastrar")
    public ApiResponseDTO<WebhookEventResponseDTO> store(@Valid @RequestBody WebhookEventCreateRequest request) {
        WebhookEvent webhookEvent = webhookEventService.createWebhookEvent(request);
        WebhookEventResponseDTO dto = new WebhookEventResponseDTO(webhookEvent.getId(), webhookEvent.getUsuarioMarketplaceId(), webhookEvent.getMarketplaceId(), webhookEvent.getTipoEvento(), webhookEvent.getPayload(), webhookEvent.getReceivedAt(), webhookEvent.getProcessed(), webhookEvent.getProcessedAt(), webhookEvent.getError());
        return new ApiResponseDTO<>(200, "WebhookEvent cadastrado com sucesso!", dto);
    }

    // PUT /api/WebhookEvent/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    public ApiResponseDTO<WebhookEventResponseDTO> update(@PathVariable int id, @Valid @RequestBody WebhookEventUpdateRequest request) {
        WebhookEvent webhookEvent = webhookEventService.updateWebhookEvent(id, request);
        if (webhookEvent == null) {
            return new ApiResponseDTO<>(404, "WebhookEvent não encontrado!", null);
        }
        WebhookEventResponseDTO dto = new WebhookEventResponseDTO(webhookEvent.getId(), webhookEvent.getUsuarioMarketplaceId(), webhookEvent.getMarketplaceId(), webhookEvent.getTipoEvento(), webhookEvent.getPayload(), webhookEvent.getReceivedAt(), webhookEvent.getProcessed(), webhookEvent.getProcessedAt(), webhookEvent.getError());
        return new ApiResponseDTO<>(200, "WebhookEvent atualizado!", dto);
    }

    // DELETE /api/WebhookEvent/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    public ApiResponseDTO<Void> destroy(@PathVariable int id) {
        boolean deleted = webhookEventService.deleteWebhookEvent(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "WebhookEvent não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "WebhookEvent deletado!", null);
    }
}
