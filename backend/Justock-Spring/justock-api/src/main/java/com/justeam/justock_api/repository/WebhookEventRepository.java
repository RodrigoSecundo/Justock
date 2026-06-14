package com.justeam.justock_api.repository;

import com.justeam.justock_api.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Integer> {

		@Query("""
						select webhookEvent.id
							from WebhookEvent webhookEvent
						 where webhookEvent.processed = false
							 and webhookEvent.usuarioMarketplaceId is not null
						 order by webhookEvent.receivedAt asc, webhookEvent.id asc
						""")
		List<Integer> findPendingProcessableIds();

		@Modifying(clearAutomatically = true, flushAutomatically = true)
		@Query("""
						update WebhookEvent webhookEvent
							 set webhookEvent.processingStartedAt = :processingStartedAt
						 where webhookEvent.id = :eventId
							 and webhookEvent.processed = false
							 and webhookEvent.usuarioMarketplaceId is not null
							 and (webhookEvent.processingStartedAt is null or webhookEvent.processingStartedAt < :staleBefore)
						""")
		int claimForProcessing(@Param("eventId") Integer eventId,
													 @Param("processingStartedAt") LocalDateTime processingStartedAt,
													 @Param("staleBefore") LocalDateTime staleBefore);
}
