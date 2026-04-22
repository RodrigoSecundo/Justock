package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
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
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Integer DEFAULT_USUARIO = 1;

    private final DashboardEventService dashboardEventService;

    public DashboardController(DashboardEventService dashboardEventService) {
        this.dashboardEventService = dashboardEventService;
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, List<Map<String, Object>>>> getRecentActivity() {
        return new ApiResponseDTO<>(200, "Atividades encontradas!",
                Map.of("activities", dashboardEventService.getRecentActivity(DEFAULT_USUARIO)));
    }

    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, List<Map<String, Object>>>> getAlerts() {
        return new ApiResponseDTO<>(200, "Alertas encontrados!",
                Map.of("alerts", dashboardEventService.getActiveAlerts(DEFAULT_USUARIO)));
    }

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, Object>> getNotifications() {
        List<Map<String, Object>> notifications = dashboardEventService.getUnreadNotifications(DEFAULT_USUARIO);
        long unreadCount = dashboardEventService.getUnreadNotificationsCount(DEFAULT_USUARIO);
        return new ApiResponseDTO<>(200, "Notificações encontradas!",
                Map.of(
                        "notifications", notifications,
                "unreadCount", unreadCount));
    }

    @PostMapping("/notifications/{eventId}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Map<String, Object>> markNotificationAsRead(@PathVariable Long eventId) {
        return new ApiResponseDTO<>(200, "Notificação marcada como visualizada!",
                dashboardEventService.markAsRead(DEFAULT_USUARIO, eventId));
    }

    @PostMapping("/activity")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<Void> registerActivity(@RequestBody Map<String, Object> payload) {
        String eventKey = String.valueOf(payload.getOrDefault("eventKey", "SETTINGS_UPDATED"));
        if ("THEME_CHANGED".equalsIgnoreCase(eventKey)) {
            dashboardEventService.recordThemeChanged(DEFAULT_USUARIO, String.valueOf(payload.getOrDefault("theme", "light")));
        } else {
            dashboardEventService.recordSettingsUpdated(DEFAULT_USUARIO, payload);
        }
        return new ApiResponseDTO<>(200, "Atividade registrada!", null);
    }
}