package com.justeam.justock_api.service;

import com.justeam.justock_api.model.WebhookEvent;
import com.justeam.justock_api.model.event.MercadoLivreWebhookReceivedEvent;
import com.justeam.justock_api.repository.WebhookEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MercadoLivreWebhookProcessor {

    private static final int MAX_BATCH_SIZE = 10;

    private final MercadoLivreService mercadoLivreService;
    private final WebhookEventRepository webhookEventRepository;

    public MercadoLivreWebhookProcessor(MercadoLivreService mercadoLivreService,
            WebhookEventRepository webhookEventRepository) {
        this.mercadoLivreService = mercadoLivreService;
        this.webhookEventRepository = webhookEventRepository;
    }

    @Async("mercadoLivreWebhookExecutor")
    @EventListener
    public void process(MercadoLivreWebhookReceivedEvent event) {
        processWebhookEvent(event.webhookEventId(), event.usuarioId());
    }

    @Scheduled(fixedDelayString = "${mercadolivre.webhook.retry.fixed-delay-ms:30000}",
            initialDelayString = "${mercadolivre.webhook.retry.initial-delay-ms:15000}")
    public void retryPendingWebhookEvents() {
        List<Integer> pendingIds = webhookEventRepository.findPendingProcessableIds();
        int processed = 0;

        for (Integer pendingId : pendingIds) {
            if (pendingId == null || processed >= MAX_BATCH_SIZE) {
                break;
            }

            WebhookEvent webhookEvent = webhookEventRepository.findById(pendingId).orElse(null);
            if (webhookEvent == null) {
                continue;
            }

            Integer usuarioId = resolveUsuarioId(webhookEvent);
            if (usuarioId == null) {
                continue;
            }

            processWebhookEvent(webhookEvent.getId(), usuarioId);
            processed++;
        }
    }

    @Transactional
    protected void processWebhookEvent(int webhookEventId, Integer usuarioId) {
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime staleBefore = startedAt.minusMinutes(5);
        int claimed = webhookEventRepository.claimForProcessing(webhookEventId, startedAt, staleBefore);
        if (claimed == 0) {
            return;
        }

        WebhookEvent webhookEvent = webhookEventRepository.findById(webhookEventId).orElse(null);
        if (webhookEvent == null) {
            return;
        }

        webhookEvent.setAttemptCount((webhookEvent.getAttemptCount() == null ? 0 : webhookEvent.getAttemptCount()) + 1);

        try {
            mercadoLivreService.syncMarketplaceData(usuarioId);
            webhookEvent.setProcessed(Boolean.TRUE);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEvent.setProcessingStartedAt(null);
            webhookEvent.setError(null);
        } catch (Exception exception) {
            webhookEvent.setProcessed(Boolean.FALSE);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEvent.setProcessingStartedAt(null);
            webhookEvent.setError(exception.getMessage());
        }

        webhookEventRepository.save(webhookEvent);
    }

    private Integer resolveUsuarioId(WebhookEvent webhookEvent) {
        return mercadoLivreService.resolveUsuarioIdFromWebhookEvent(webhookEvent.getUsuarioMarketplaceId());
    }
}