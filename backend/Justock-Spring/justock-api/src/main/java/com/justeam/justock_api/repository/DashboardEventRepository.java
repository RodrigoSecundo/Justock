package com.justeam.justock_api.repository;

import com.justeam.justock_api.model.DashboardEvent;
import com.justeam.justock_api.model.DashboardEventScope;
import com.justeam.justock_api.model.DashboardEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardEventRepository extends JpaRepository<DashboardEvent, Long> {
    List<DashboardEvent> findTop5ByUsuarioAndEventScopeOrderByCreatedAtDesc(Integer usuario, DashboardEventScope eventScope);
    List<DashboardEvent> findTop5ByUsuarioAndEventScopeAndResolvedAtIsNullOrderByCreatedAtDesc(Integer usuario, DashboardEventScope eventScope);
    List<DashboardEvent> findByUsuarioAndEventScopeAndResolvedAtIsNullOrderByCreatedAtDesc(Integer usuario, DashboardEventScope eventScope);
    List<DashboardEvent> findTop5ByUsuarioAndEventScopeAndResolvedAtIsNullAndReadAtIsNullOrderByCreatedAtDesc(Integer usuario, DashboardEventScope eventScope);
    long countByUsuarioAndEventScopeAndResolvedAtIsNullAndReadAtIsNull(Integer usuario, DashboardEventScope eventScope);
    Optional<DashboardEvent> findFirstByUsuarioAndEventScopeAndEventTypeAndResourceTypeAndResourceIdAndResolvedAtIsNull(
            Integer usuario,
            DashboardEventScope eventScope,
            DashboardEventType eventType,
            String resourceType,
            String resourceId);
}
