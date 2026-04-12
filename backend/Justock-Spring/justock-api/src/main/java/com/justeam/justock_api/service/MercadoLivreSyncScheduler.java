package com.justeam.justock_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MercadoLivreSyncScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoLivreSyncScheduler.class);

    private final MercadoLivreService mercadoLivreService;

    @Value("${mercadolivre.auto-sync.enabled:true}")
    private boolean autoSyncEnabled;

    public MercadoLivreSyncScheduler(MercadoLivreService mercadoLivreService) {
        this.mercadoLivreService = mercadoLivreService;
    }

    @Scheduled(
            fixedDelayString = "${mercadolivre.auto-sync.fixed-delay-ms:900000}",
            initialDelayString = "${mercadolivre.auto-sync.initial-delay-ms:120000}")
    public void syncMercadoLivrePeriodicamente() {
        if (!autoSyncEnabled) {
            return;
        }

        Integer usuarioId = mercadoLivreService.getIntegrationUserId();
        if (!mercadoLivreService.isConnected(usuarioId)) {
            return;
        }

        try {
            mercadoLivreService.syncMarketplaceData(usuarioId);
        } catch (Exception exception) {
            LOGGER.warn("Falha na sincronização automática do Mercado Livre: {}", exception.getMessage());
        }
    }
}