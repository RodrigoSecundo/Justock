package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.service.CurrentAccountService;
import com.justeam.justock_api.service.DashboardEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardEventService dashboardEventService;
    private final CurrentAccountService currentAccountService;

    public DashboardController(DashboardEventService dashboardEventService, CurrentAccountService currentAccountService) {
        this.dashboardEventService = dashboardEventService;
        this.currentAccountService = currentAccountService;
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, List<Map<String, Object>>>> getRecentActivity() {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        return new ApiResponseDTO<>(200, "Atividades encontradas!",
            Map.of("activities", dashboardEventService.getRecentActivity(usuarioId)));
    }

    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, List<Map<String, Object>>>> getAlerts() {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        return new ApiResponseDTO<>(200, "Alertas encontrados!",
            Map.of("alerts", dashboardEventService.getActiveAlerts(usuarioId)));
    }

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, Object>> getNotifications() {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        List<Map<String, Object>> notifications = dashboardEventService.getUnreadNotifications(usuarioId);
        long unreadCount = dashboardEventService.getUnreadNotificationsCount(usuarioId);
        return new ApiResponseDTO<>(200, "Notificações encontradas!",
                Map.of(
                        "notifications", notifications,
                "unreadCount", unreadCount));
    }

    @PostMapping("/notifications/{eventId}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, Object>> markNotificationAsRead(@PathVariable Long eventId) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        return new ApiResponseDTO<>(200, "Notificação marcada como visualizada!",
            dashboardEventService.markAsRead(usuarioId, eventId));
    }

    @PostMapping("/activity")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Void> registerActivity(@RequestBody Map<String, Object> payload) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        String eventKey = String.valueOf(payload.getOrDefault("eventKey", "SETTINGS_UPDATED"));
        if ("THEME_CHANGED".equalsIgnoreCase(eventKey)) {
            dashboardEventService.recordThemeChanged(usuarioId, String.valueOf(payload.getOrDefault("theme", "light")));
        } else {
            dashboardEventService.recordSettingsUpdated(usuarioId, payload);
        }
        return new ApiResponseDTO<>(200, "Atividade registrada!", null);
    }
}