package com.justeam.justock_api.service;

import com.justeam.justock_api.model.WebhookEvent;
import com.justeam.justock_api.model.event.MercadoLivreWebhookReceivedEvent;
import com.justeam.justock_api.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoLivreWebhookProcessorTest {

    @Mock
    private MercadoLivreService mercadoLivreService;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @InjectMocks
    private MercadoLivreWebhookProcessor mercadoLivreWebhookProcessor;

    @Test
    void processMarksEventAsProcessedWhenSyncSucceeds() {
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setId(15);
        webhookEvent.setProcessed(Boolean.FALSE);
        webhookEvent.setAttemptCount(0);

        when(webhookEventRepository.claimForProcessing(eq(15), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(webhookEventRepository.findById(15)).thenReturn(Optional.of(webhookEvent));
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mercadoLivreWebhookProcessor.process(new MercadoLivreWebhookReceivedEvent(15, 1));

        ArgumentCaptor<WebhookEvent> eventCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).save(eventCaptor.capture());
        assertTrue(Boolean.TRUE.equals(eventCaptor.getValue().getProcessed()));
        assertNotNull(eventCaptor.getValue().getProcessedAt());
        assertNull(eventCaptor.getValue().getError());
        assertEquals(1, eventCaptor.getValue().getAttemptCount());
    }

    @Test
    void processStoresFailureWhenSyncThrowsException() {
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setId(16);
        webhookEvent.setProcessed(Boolean.FALSE);
        webhookEvent.setAttemptCount(0);

        when(webhookEventRepository.claimForProcessing(eq(16), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(webhookEventRepository.findById(16)).thenReturn(Optional.of(webhookEvent));
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Falha temporaria")).when(mercadoLivreService).syncMarketplaceData(1);

        mercadoLivreWebhookProcessor.process(new MercadoLivreWebhookReceivedEvent(16, 1));

        ArgumentCaptor<WebhookEvent> eventCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).save(eventCaptor.capture());
        assertTrue(Boolean.FALSE.equals(eventCaptor.getValue().getProcessed()));
        assertNotNull(eventCaptor.getValue().getProcessedAt());
        assertTrue(eventCaptor.getValue().getError().contains("Falha temporaria"));
        assertEquals(1, eventCaptor.getValue().getAttemptCount());
    }

    @Test
    void retryPendingWebhookEventsReprocessesQueuedEvents() {
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setId(18);
        webhookEvent.setUsuarioMarketplaceId(99);
        webhookEvent.setProcessed(Boolean.FALSE);
        webhookEvent.setAttemptCount(1);

        when(webhookEventRepository.findPendingProcessableIds()).thenReturn(List.of(18));
        when(webhookEventRepository.findById(18)).thenReturn(Optional.of(webhookEvent));
        when(webhookEventRepository.claimForProcessing(eq(18), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mercadoLivreService.resolveUsuarioIdFromWebhookEvent(99)).thenReturn(1);

        mercadoLivreWebhookProcessor.retryPendingWebhookEvents();

        verify(mercadoLivreService).syncMarketplaceData(1);
        ArgumentCaptor<WebhookEvent> eventCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).save(eventCaptor.capture());
        assertEquals(2, eventCaptor.getValue().getAttemptCount());
        assertTrue(Boolean.TRUE.equals(eventCaptor.getValue().getProcessed()));
    }

    @Test
    void processSkipsEventWhenClaimFails() {
        when(webhookEventRepository.claimForProcessing(eq(20), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0);

        mercadoLivreWebhookProcessor.process(new MercadoLivreWebhookReceivedEvent(20, 1));

        verify(webhookEventRepository, never()).save(any(WebhookEvent.class));
        verify(mercadoLivreService, never()).syncMarketplaceData(any());
    }
}