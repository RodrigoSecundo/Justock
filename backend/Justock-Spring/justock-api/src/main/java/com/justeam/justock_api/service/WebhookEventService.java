package com.justeam.justock_api.service;

import com.justeam.justock_api.model.WebhookEvent;
import com.justeam.justock_api.repository.WebhookEventRepository;
import com.justeam.justock_api.request.WebhookEventCreateRequest;
import com.justeam.justock_api.request.WebhookEventUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class WebhookEventService {

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    public List<WebhookEvent> listAllWebhookEvents() {
        return webhookEventRepository.findAll();
    }

    public WebhookEvent findWebhookEvent(int id) {
        return webhookEventRepository.findById(id).orElse(null);
    }

    public WebhookEvent createWebhookEvent(WebhookEventCreateRequest dto) {
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
        webhookEvent.setMarketplaceId(dto.getMarketplaceId());
        webhookEvent.setTipoEvento(dto.getTipoEvento());
        webhookEvent.setPayload(dto.getPayload());
        webhookEvent.setReceivedAt(dto.getReceivedAt());
        webhookEvent.setProcessed(dto.getProcessed());
        webhookEvent.setProcessedAt(dto.getProcessedAt());
        webhookEvent.setError(dto.getError());
        return webhookEventRepository.save(webhookEvent);
    }

    @Transactional
    public WebhookEvent updateWebhookEvent(int id, WebhookEventUpdateRequest dto) {
        WebhookEvent existingWebhookEvent = webhookEventRepository.findById(id).orElse(null);
        if (existingWebhookEvent != null) {
            existingWebhookEvent.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
            existingWebhookEvent.setMarketplaceId(dto.getMarketplaceId());
            existingWebhookEvent.setTipoEvento(dto.getTipoEvento());
            existingWebhookEvent.setPayload(dto.getPayload());
            existingWebhookEvent.setReceivedAt(dto.getReceivedAt());
            existingWebhookEvent.setProcessed(dto.getProcessed());
            existingWebhookEvent.setProcessedAt(dto.getProcessedAt());
            existingWebhookEvent.setError(dto.getError());
            return webhookEventRepository.save(existingWebhookEvent);
        }
        return null;
    }

    public boolean deleteWebhookEvent(int id) {
        if (webhookEventRepository.existsById(id)) {
            webhookEventRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
