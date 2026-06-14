package com.justeam.justock_api.model.event;

public record MercadoLivreWebhookReceivedEvent(int webhookEventId, Integer usuarioId) {
}