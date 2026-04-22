package com.justeam.justock_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justeam.justock_api.exception.ResourceNotFoundException;
import com.justeam.justock_api.model.DashboardEvent;
import com.justeam.justock_api.model.DashboardEventScope;
import com.justeam.justock_api.model.DashboardEventType;
import com.justeam.justock_api.model.Order;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.repository.DashboardEventRepository;
import com.justeam.justock_api.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DashboardEventService {

    public static final int LOW_STOCK_THRESHOLD = 3;

    private final DashboardEventRepository dashboardEventRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public DashboardEventService(
            DashboardEventRepository dashboardEventRepository,
            ProductRepository productRepository,
            ObjectMapper objectMapper) {
        this.dashboardEventRepository = dashboardEventRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> getRecentActivity(Integer usuario) {
        return dashboardEventRepository.findTop5ByUsuarioAndEventScopeOrderByCreatedAtDesc(usuario, DashboardEventScope.ACTIVITY)
                .stream()
                .map(this::toFeedItem)
                .toList();
    }

    public List<Map<String, Object>> getActiveAlerts(Integer usuario) {
        reconcileInventoryAlerts(usuario);
        return dashboardEventRepository.findTop5ByUsuarioAndEventScopeAndResolvedAtIsNullOrderByCreatedAtDesc(usuario, DashboardEventScope.ALERT)
                .stream()
                .map(this::toFeedItem)
                .toList();
    }

    public List<Map<String, Object>> getUnreadNotifications(Integer usuario) {
        reconcileInventoryAlerts(usuario);
        return dashboardEventRepository.findTop5ByUsuarioAndEventScopeAndResolvedAtIsNullAndReadAtIsNullOrderByCreatedAtDesc(usuario, DashboardEventScope.ALERT)
                .stream()
                .map(this::toNotificationItem)
                .toList();
    }

    public long getUnreadNotificationsCount(Integer usuario) {
        reconcileInventoryAlerts(usuario);
        return dashboardEventRepository.countByUsuarioAndEventScopeAndResolvedAtIsNullAndReadAtIsNull(usuario, DashboardEventScope.ALERT);
    }

    public Map<String, Object> markAsRead(Integer usuario, Long eventId) {
        DashboardEvent event = dashboardEventRepository.findById(eventId)
                .filter(existing -> Objects.equals(existing.getUsuario(), usuario))
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada."));
        event.setReadAt(LocalDateTime.now());
        dashboardEventRepository.save(event);
        return toNotificationItem(event);
    }

    public void recordProductCreated(Product product) {
        recordActivity(product.getUsuario(), DashboardEventType.PRODUCT_CREATED,
                "Produto adicionado",
                "Produto " + safeName(product.getNomeDoProduto()) + " foi cadastrado.",
                "product",
                String.valueOf(product.getIdProduto()),
                Map.of("quantity", safeQuantity(product.getQuantidade())));
        refreshStockAlerts(product);
    }

    public void recordProductUpdated(Product previousProduct, Product product) {
        recordActivity(product.getUsuario(), DashboardEventType.PRODUCT_UPDATED,
                "Produto atualizado",
                "Produto " + safeName(product.getNomeDoProduto()) + " foi atualizado.",
                "product",
                String.valueOf(product.getIdProduto()),
                Map.of(
                        "previousQuantity", safeQuantity(previousProduct.getQuantidade()),
                        "currentQuantity", safeQuantity(product.getQuantidade())));
        refreshStockAlerts(product);
    }

    public void recordProductDeleted(Product product) {
        resolveOpenAlert(product.getUsuario(), DashboardEventType.PRODUCT_LOW_STOCK, "product", String.valueOf(product.getIdProduto()));
        resolveOpenAlert(product.getUsuario(), DashboardEventType.PRODUCT_OUT_OF_STOCK, "product", String.valueOf(product.getIdProduto()));
        recordActivity(product.getUsuario(), DashboardEventType.PRODUCT_DELETED,
                "Produto removido",
                "Produto " + safeName(product.getNomeDoProduto()) + " foi excluído.",
                "product",
                String.valueOf(product.getIdProduto()),
                Map.of());
    }

    public void recordOrderCreated(Order order) {
        recordActivity(resolveOrderUsuario(order), DashboardEventType.ORDER_CREATED,
                "Pedido adicionado",
                "Pedido #" + order.getIdPedido() + " foi cadastrado.",
                "order",
                String.valueOf(order.getIdPedido()),
                Map.of("status", safeText(order.getStatusPedido())));
    }

    public void recordOrderUpdated(Order order) {
        recordActivity(resolveOrderUsuario(order), DashboardEventType.ORDER_UPDATED,
                "Pedido atualizado",
                "Pedido #" + order.getIdPedido() + " foi atualizado.",
                "order",
                String.valueOf(order.getIdPedido()),
                Map.of("status", safeText(order.getStatusPedido())));
    }

    public void recordOrderDeleted(Order order) {
        recordActivity(resolveOrderUsuario(order), DashboardEventType.ORDER_DELETED,
                "Pedido removido",
                "Pedido #" + order.getIdPedido() + " foi excluído.",
                "order",
                String.valueOf(order.getIdPedido()),
                Map.of());
    }

    public void recordSyncedProduct(Product product, boolean created) {
        recordActivity(product.getUsuario(), DashboardEventType.PRODUCT_SYNCED,
                created ? "Produto sincronizado" : "Produto atualizado via integração",
                created
                ? "Produto " + safeName(product.getNomeDoProduto()) + " entrou via integração."
                        : "Produto " + safeName(product.getNomeDoProduto()) + " foi atualizado pela integração.",
                "product",
                String.valueOf(product.getIdProduto()),
                Map.of("source", safeText(product.getMarketplaceSource())));
        refreshStockAlerts(product);
    }

    public void recordSyncedOrder(Order order, boolean created) {
        Integer usuario = resolveOrderUsuario(order);
        recordActivity(usuario, DashboardEventType.ORDER_SYNCED,
                created ? "Pedido sincronizado" : "Pedido atualizado via integração",
                created
                        ? "Pedido #" + order.getIdPedido() + " entrou via integração."
                        : "Pedido #" + order.getIdPedido() + " foi atualizado pela integração.",
                "order",
                String.valueOf(order.getIdPedido()),
                Map.of("source", safeText(order.getMarketplaceSource())));
    }

    public void recordSyncCompleted(Integer usuario, int productCount, int orderCount) {
        recordActivity(usuario, DashboardEventType.SYNC_COMPLETED,
                "Sincronização concluída",
                "Sincronização finalizada com " + productCount + " produtos e " + orderCount + " pedidos processados.",
                "sync",
                usuario + ":sync",
                Map.of("products", productCount, "orders", orderCount));
        reconcileInventoryAlerts(usuario);
    }

    public void recordSettingsUpdated(Integer usuario, Map<String, Object> changes) {
        recordActivity(usuario, DashboardEventType.SETTINGS_UPDATED,
                "Configurações salvas",
                "Preferências do dashboard foram atualizadas.",
                "settings",
                String.valueOf(usuario),
                changes);
    }

    public void recordThemeChanged(Integer usuario, String theme) {
        recordActivity(usuario, DashboardEventType.THEME_CHANGED,
                "Tema alterado",
                "Tema alterado para " + safeText(theme) + ".",
                "theme",
                String.valueOf(usuario),
                Map.of("theme", safeText(theme)));
    }

    public void reconcileInventoryAlerts(Integer usuario) {
        List<Product> products = productRepository.findByUsuario(usuario);
        for (Product product : products) {
            refreshStockAlerts(product);
        }
    }

    private void refreshStockAlerts(Product product) {
        int quantity = safeQuantity(product.getQuantidade());
        Integer usuario = product.getUsuario();
        String resourceId = String.valueOf(product.getIdProduto());

        if (quantity <= 0) {
            upsertAlert(usuario, DashboardEventType.PRODUCT_OUT_OF_STOCK,
                    "Produto esgotado",
                    "Produto " + safeName(product.getNomeDoProduto()) + " está com estoque zerado.",
                    "product",
                    resourceId,
                    Map.of("quantity", quantity));
            resolveOpenAlert(usuario, DashboardEventType.PRODUCT_LOW_STOCK, "product", resourceId);
            return;
        }

        resolveOpenAlert(usuario, DashboardEventType.PRODUCT_OUT_OF_STOCK, "product", resourceId);
        if (quantity < LOW_STOCK_THRESHOLD) {
            upsertAlert(usuario, DashboardEventType.PRODUCT_LOW_STOCK,
                    "Estoque em baixa",
                    "Produto " + safeName(product.getNomeDoProduto()) + " está com apenas " + quantity + " unidade(s).",
                    "product",
                    resourceId,
                    Map.of("quantity", quantity));
        } else {
            resolveOpenAlert(usuario, DashboardEventType.PRODUCT_LOW_STOCK, "product", resourceId);
        }
    }

    private void recordActivity(Integer usuario, DashboardEventType type, String title, String description,
            String resourceType, String resourceId, Map<String, Object> metadata) {
        DashboardEvent event = new DashboardEvent();
        event.setUsuario(usuario);
        event.setEventScope(DashboardEventScope.ACTIVITY);
        event.setEventType(type);
        event.setTitle(title);
        event.setDescription(description);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setCreatedAt(LocalDateTime.now());
        event.setMetadataJson(writeMetadata(metadata));
        dashboardEventRepository.save(event);
    }

    private void upsertAlert(Integer usuario, DashboardEventType type, String title, String description,
            String resourceType, String resourceId, Map<String, Object> metadata) {
        DashboardEvent event = dashboardEventRepository
                .findFirstByUsuarioAndEventScopeAndEventTypeAndResourceTypeAndResourceIdAndResolvedAtIsNull(
                        usuario,
                        DashboardEventScope.ALERT,
                        type,
                        resourceType,
                        resourceId)
                .orElseGet(DashboardEvent::new);
        if (event.getId() == null) {
            event.setUsuario(usuario);
            event.setEventScope(DashboardEventScope.ALERT);
            event.setEventType(type);
            event.setResourceType(resourceType);
            event.setResourceId(resourceId);
            event.setCreatedAt(LocalDateTime.now());
        }
        event.setTitle(title);
        event.setDescription(description);
        event.setResolvedAt(null);
        event.setMetadataJson(writeMetadata(metadata));
        dashboardEventRepository.save(event);
    }

    private void resolveOpenAlert(Integer usuario, DashboardEventType type, String resourceType, String resourceId) {
        dashboardEventRepository
                .findFirstByUsuarioAndEventScopeAndEventTypeAndResourceTypeAndResourceIdAndResolvedAtIsNull(
                        usuario,
                        DashboardEventScope.ALERT,
                        type,
                        resourceType,
                        resourceId)
                .ifPresent(event -> {
                    event.setResolvedAt(LocalDateTime.now());
                    dashboardEventRepository.save(event);
                });
    }

    private Map<String, Object> toFeedItem(DashboardEvent event) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", event.getId());
        item.put("type", mapFrontendType(event.getEventType()));
        item.put("title", event.getTitle());
        item.put("text", event.getDescription());
        item.put("time", formatRelativeTime(event.getCreatedAt()));
        item.put("createdAt", event.getCreatedAt());
        return item;
    }

    private Map<String, Object> toNotificationItem(DashboardEvent event) {
        Map<String, Object> item = toFeedItem(event);
        item.put("read", event.getReadAt() != null);
        return item;
    }

    private String mapFrontendType(DashboardEventType eventType) {
        return switch (eventType) {
            case PRODUCT_LOW_STOCK -> "baixo";
            case PRODUCT_OUT_OF_STOCK -> "fora-estoque";
            case PRODUCT_DELETED, ORDER_DELETED -> "reembolsado";
            default -> "atualizado";
        };
    }

    private String formatRelativeTime(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 1) {
            return "agora";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " h";
        }
        long days = duration.toDays();
        return days + " d";
    }

    private Integer resolveOrderUsuario(Order order) {
        return 1;
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    private String safeName(String value) {
        String trimmed = safeText(value);
        return trimmed.isBlank() ? "sem nome" : trimmed;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
