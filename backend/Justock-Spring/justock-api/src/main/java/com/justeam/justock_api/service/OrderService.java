package com.justeam.justock_api.service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.justeam.justock_api.exception.BadRequestException;
import com.justeam.justock_api.dto.OrderItemResponseDTO;
import com.justeam.justock_api.model.OrderItem;
import com.justeam.justock_api.model.OrderItemId;
import com.justeam.justock_api.model.Order;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.model.UserMarketplace;
import com.justeam.justock_api.repository.OrderItemRepository;
import com.justeam.justock_api.repository.OrderRepository;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.repository.UserMarketplaceRepository;
import com.justeam.justock_api.request.OrderCreateRequest;
import com.justeam.justock_api.request.OrderItemRequest;
import com.justeam.justock_api.request.OrderUpdateRequest;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    private static final Set<String> ALLOWED_ORDER_STATUS = Set.of("EM ANDAMENTO", "CANCELADO", "CONCLUÍDO");
    private static final Set<String> ALLOWED_PAYMENT_STATUS = Set.of("PROCESSADO", "EM PROCESSAMENTO", "CANCELADO", "NEGADO");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserMarketplaceRepository userMarketplaceRepository;
    private final DashboardEventService dashboardEventService;
    private final MercadoLivreService mercadoLivreService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            UserMarketplaceRepository userMarketplaceRepository,
            DashboardEventService dashboardEventService,
            MercadoLivreService mercadoLivreService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userMarketplaceRepository = userMarketplaceRepository;
        this.dashboardEventService = dashboardEventService;
        this.mercadoLivreService = mercadoLivreService;
    }

    public List<Order> listAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> listOrdersByUsuario(Integer usuarioId, boolean includeAllOrders) {
        if (includeAllOrders) {
            return orderRepository.findAll();
        }

        List<Integer> marketplaceIds = userMarketplaceRepository.findByUsuario(usuarioId)
                .stream()
                .map(UserMarketplace::getUsuarioMarketplaceId)
                .toList();

        if (marketplaceIds.isEmpty()) {
            return List.of();
        }

        return orderRepository.findByUsuarioMarketplaceIdIn(marketplaceIds);
    }

    public Order findOrder(int id) {
        return orderRepository.findById(id).orElse(null);
    }

    public Order findOrderVisibleToUsuario(int id, Integer usuarioId, boolean includeAllOrders) {
        Order order = findOrder(id);
        if (order == null) {
            return null;
        }
        if (includeAllOrders) {
            return order;
        }

        Set<Integer> marketplaceIds = new HashSet<>(userMarketplaceRepository.findByUsuario(usuarioId)
                .stream()
                .map(UserMarketplace::getUsuarioMarketplaceId)
                .toList());

        if (order.getUsuarioMarketplaceId() == null || !marketplaceIds.contains(order.getUsuarioMarketplaceId())) {
            return null;
        }

        return order;
    }

    @Transactional
    public Order createOrder(OrderCreateRequest dto, Integer usuarioId) {
        validateOrderDates(dto.getDataEmissao(), dto.getDataEntrega());
        String normalizedStatus = normalizeOrderStatus(dto.getStatusPedido());
        String normalizedPaymentStatus = normalizePaymentStatus(dto.getStatusPagamento());
        String marketplaceSource = resolveMarketplaceSource(dto.getIdPedidoMarketplace());
        List<OrderItem> orderItems = buildManualOrderItems(usuarioId, dto.getItens(), marketplaceSource, null);

        Order order = new Order();
        order.setIdPedidoMarketplace(dto.getIdPedidoMarketplace());
        order.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
        order.setDataEntrega(dto.getDataEntrega());
        order.setDataEmissao(dto.getDataEmissao());
        order.setStatusPagamento(normalizedPaymentStatus);
        order.setStatusPedido(normalizedStatus);
        order.setMarketplaceSource(marketplaceSource);
        order.setMarketplaceResourceId(null);
        order.setObservacao(normalizeObservation(dto.getObservacao()));
        order.setInventoryApplied(Boolean.FALSE);
        Order savedOrder = orderRepository.save(order);
        persistOrderItems(savedOrder.getIdPedido(), orderItems);
        reconcileManualOrderInventory(savedOrder, orderItemRepository.findByIdIdPedido(savedOrder.getIdPedido()), usuarioId);
        dashboardEventService.recordOrderCreated(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order updateOrder(int id, OrderUpdateRequest dto, Integer usuarioId) {
        validateOrderDates(dto.getDataEmissao(), dto.getDataEntrega());
        String normalizedStatus = normalizeOrderStatus(dto.getStatusPedido());
        String normalizedPaymentStatus = normalizePaymentStatus(dto.getStatusPagamento());

        Order existingOrder = orderRepository.findById(id).orElse(null);
        if (existingOrder != null) {
            ensureManualOrder(existingOrder);
            List<OrderItem> currentItems = orderItemRepository.findByIdIdPedido(existingOrder.getIdPedido());
            boolean itemsProvided = dto.getItens() != null;

            if (itemsProvided && Boolean.TRUE.equals(existingOrder.getInventoryApplied())) {
                applyInventoryAdjustment(currentItems, 1, usuarioId);
                existingOrder.setInventoryApplied(Boolean.FALSE);
            }

            existingOrder.setIdPedidoMarketplace(dto.getIdPedidoMarketplace());
            existingOrder.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
            existingOrder.setDataEntrega(dto.getDataEntrega());
            existingOrder.setDataEmissao(dto.getDataEmissao());
            existingOrder.setStatusPagamento(normalizedPaymentStatus);
            existingOrder.setStatusPedido(normalizedStatus);
            existingOrder.setMarketplaceSource(resolveMarketplaceSource(dto.getIdPedidoMarketplace()));
            existingOrder.setObservacao(normalizeObservation(dto.getObservacao()));
            Order updatedOrder = orderRepository.save(existingOrder);

            List<OrderItem> effectiveItems = currentItems;
            if (itemsProvided) {
                List<OrderItem> rebuiltItems = buildManualOrderItems(usuarioId, dto.getItens(), updatedOrder.getMarketplaceSource(), updatedOrder.getIdPedido());
                orderItemRepository.deleteByIdIdPedido(updatedOrder.getIdPedido());
                persistOrderItems(updatedOrder.getIdPedido(), rebuiltItems);
                effectiveItems = orderItemRepository.findByIdIdPedido(updatedOrder.getIdPedido());
            }

            reconcileManualOrderInventory(updatedOrder, effectiveItems, usuarioId);
            dashboardEventService.recordOrderUpdated(updatedOrder);
            return updatedOrder;
        }
        return null;
    }

    @Transactional
    public boolean deleteOrder(int id, Integer usuarioId) {
        Order existingOrder = orderRepository.findById(id).orElse(null);
        if (existingOrder != null) {
            ensureManualOrder(existingOrder);
            if (Boolean.TRUE.equals(existingOrder.getInventoryApplied())) {
                applyInventoryAdjustment(orderItemRepository.findByIdIdPedido(existingOrder.getIdPedido()), 1, usuarioId);
            }
            orderItemRepository.deleteByIdIdPedido(existingOrder.getIdPedido());
            orderRepository.deleteById(id);
            dashboardEventService.recordOrderDeleted(existingOrder);
            return true;
        }
        return false;
    }

    private void validateOrderDates(LocalDate dataEmissao, LocalDate dataEntrega) {
        if (dataEmissao == null) {
            throw new BadRequestException("A data de emissão é obrigatória.");
        }

        LocalDate today = LocalDate.now();
        if (dataEmissao.isAfter(today)) {
            throw new BadRequestException("A data de emissão não pode ser futura.");
        }

        if (dataEntrega != null && dataEntrega.isBefore(dataEmissao)) {
            throw new BadRequestException("A data de entrega não pode ser anterior à data de emissão.");
        }

        if (dataEntrega != null && dataEntrega.isAfter(today)) {
            throw new BadRequestException("A data de entrega não pode ser futura.");
        }
    }

    private String normalizeOrderStatus(String statusPedido) {
        if (statusPedido == null || statusPedido.trim().isEmpty()) {
            throw new BadRequestException("O status do pedido é obrigatório.");
        }

        String normalizedStatus = statusPedido.trim().toUpperCase();
        if (!ALLOWED_ORDER_STATUS.contains(normalizedStatus)) {
            throw new BadRequestException("O status do pedido deve ser EM ANDAMENTO, CANCELADO ou CONCLUÍDO.");
        }

        return normalizedStatus;
    }

    private String normalizePaymentStatus(String statusPagamento) {
        if (statusPagamento == null || statusPagamento.trim().isEmpty()) {
            throw new BadRequestException("O status de pagamento é obrigatório.");
        }

        String normalizedPaymentStatus = statusPagamento.trim().toUpperCase();
        if (!ALLOWED_PAYMENT_STATUS.contains(normalizedPaymentStatus)) {
            throw new BadRequestException("O status de pagamento deve ser PROCESSADO, EM PROCESSAMENTO, CANCELADO ou NEGADO.");
        }

        return normalizedPaymentStatus;
    }

    private String resolveMarketplaceSource(Integer marketplaceCode) {
        if (marketplaceCode == null) {
            return null;
        }

        return switch (marketplaceCode) {
            case 1 -> "MERCADO_LIVRE";
            case 2 -> "AMAZON";
            case 3 -> "SHOPEE";
            case 4 -> "MANUAL";
            default -> null;
        };
    }

    private String normalizeObservation(String observacao) {
        if (observacao == null) {
            return null;
        }

        String trimmedObservation = observacao.trim();
        return trimmedObservation.isEmpty() ? null : trimmedObservation;
    }

    private void ensureManualOrder(Order order) {
        boolean marketplaceManaged = order.getMarketplaceSource() != null
                && !order.getMarketplaceSource().isBlank()
                && !"MANUAL".equalsIgnoreCase(order.getMarketplaceSource());
        if (marketplaceManaged) {
            throw new BadRequestException("Pedidos sincronizados de marketplace não podem ser editados ou excluídos manualmente.");
        }
    }

    public List<OrderItemResponseDTO> listOrderItemResponses(int orderId) {
        Map<Integer, Product> productById = new HashMap<>();
        List<OrderItemResponseDTO> items = new ArrayList<>();
        for (OrderItem item : orderItemRepository.findByIdIdPedido(orderId)) {
            Integer productId = item.getId() == null ? null : item.getId().getIdProduto();
            if (productId != null && !productById.containsKey(productId)) {
                productRepository.findById(productId).ifPresent(product -> productById.put(productId, product));
            }
            Product product = productId == null ? null : productById.get(productId);
            items.add(new OrderItemResponseDTO(
                    productId,
                    product == null ? null : product.getNomeDoProduto(),
                    item.getQuantidade(),
                    item.getPrecoUnitario(),
                    item.getSubtotal(),
                    item.getIdItemMarketplace(),
                    item.getItemStatus()));
        }
        return items;
    }

    public BigDecimal calculateOrderTotal(int orderId) {
        return orderItemRepository.findByIdIdPedido(orderId)
                .stream()
                .map(OrderItem::getSubtotal)
                .filter(subtotal -> subtotal != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderItem> buildManualOrderItems(Integer usuarioId, List<OrderItemRequest> requests, String marketplaceSource, Integer orderId) {
        if (!"MANUAL".equalsIgnoreCase(String.valueOf(marketplaceSource))) {
            return List.of();
        }
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Selecione ao menos um produto para o pedido manual.");
        }

        Set<Integer> seenProductIds = new HashSet<>();
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest request : requests) {
            Integer productId = request.getIdProduto();
            Integer quantidade = request.getQuantidade();
            if (productId == null) {
                throw new BadRequestException("Todos os itens do pedido precisam de um produto válido.");
            }
            if (quantidade == null || quantidade <= 0) {
                throw new BadRequestException("A quantidade de cada item do pedido deve ser maior que zero.");
            }
            if (!seenProductIds.add(productId)) {
                throw new BadRequestException("Não repita o mesmo produto mais de uma vez no pedido manual.");
            }

            Product product = productRepository.findById(productId)
                    .filter(existing -> usuarioId.equals(existing.getUsuario()))
                    .orElseThrow(() -> new BadRequestException("Um dos produtos selecionados não pertence ao usuário atual."));

            if (!isManualSellableInventoryProduct(product)) {
                throw new BadRequestException("Somente produtos internos ou anúncios separados com estoque próprio podem ser usados em pedidos manuais.");
            }

            BigDecimal precoUnitario = request.getPrecoUnitario() == null ? product.getPreco() : request.getPrecoUnitario();
            if (precoUnitario == null || precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("O preço unitário do item do pedido é inválido.");
            }

            OrderItem item = new OrderItem();
            OrderItemId itemId = new OrderItemId();
            itemId.setIdProduto(productId);
            itemId.setIdPedido(orderId);
            item.setId(itemId);
            item.setQuantidade(quantidade);
            item.setPrecoUnitario(precoUnitario);
            item.setSubtotal(precoUnitario.multiply(BigDecimal.valueOf(quantidade.longValue())));
            item.setIdItemMarketplace(null);
            item.setItemStatus("MANUAL");
            items.add(item);
        }

        return items;
    }

    private void persistOrderItems(Integer orderId, List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getId() != null) {
                item.getId().setIdPedido(orderId);
            }
            orderItemRepository.save(item);
        }
    }

    private void reconcileManualOrderInventory(Order order, List<OrderItem> items, Integer usuarioId) {
        boolean shouldApply = shouldApplyInventory(order);
        boolean inventoryAlreadyApplied = Boolean.TRUE.equals(order.getInventoryApplied());

        if (shouldApply && !inventoryAlreadyApplied) {
            applyInventoryAdjustment(items, -1, usuarioId);
            order.setInventoryApplied(Boolean.TRUE);
            orderRepository.save(order);
            return;
        }

        if (!shouldApply && inventoryAlreadyApplied) {
            applyInventoryAdjustment(items, 1, usuarioId);
            order.setInventoryApplied(Boolean.FALSE);
            orderRepository.save(order);
        }
    }

    private boolean isManualSellableInventoryProduct(Product product) {
        if (product == null) {
            return false;
        }
        String marker = String.valueOf(product.getMarcador());
        if (ProductService.LISTING_INVENTORY_MARKER.equalsIgnoreCase(marker)) {
            return true;
        }
        return (product.getMarketplaceSource() == null || product.getMarketplaceSource().isBlank())
                && (product.getMarketplaceResourceId() == null || product.getMarketplaceResourceId().isBlank())
                && !"ML".equalsIgnoreCase(marker);
    }

    private boolean shouldApplyInventory(Order order) {
        return "MANUAL".equalsIgnoreCase(String.valueOf(order.getMarketplaceSource()))
                && !"CANCELADO".equalsIgnoreCase(order.getStatusPedido());
    }

    private void applyInventoryAdjustment(List<OrderItem> items, int direction, Integer usuarioId) {
        for (OrderItem item : items) {
            Integer productId = item.getId() == null ? null : item.getId().getIdProduto();
            if (productId == null) {
                continue;
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BadRequestException("Produto do pedido não encontrado para ajuste de estoque."));

            if (usuarioId != null && !usuarioId.equals(product.getUsuario())) {
                throw new BadRequestException("Produto do pedido não pertence ao usuário atual.");
            }

            int currentQuantity = product.getQuantidade() == null ? 0 : product.getQuantidade();
            int delta = (item.getQuantidade() == null ? 0 : item.getQuantidade()) * direction;
            int nextQuantity = currentQuantity + delta;
            if (nextQuantity < 0) {
                throw new BadRequestException("Estoque insuficiente para concluir o pedido manual.");
            }

            product.setQuantidade(nextQuantity);
            productRepository.save(product);
            mercadoLivreService.syncManualInventoryForProduct(product);
        }
    }
}
