package com.justeam.justock_api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.justeam.justock_api.exception.BadRequestException;
import com.justeam.justock_api.model.Order;
import com.justeam.justock_api.repository.OrderRepository;
import com.justeam.justock_api.request.OrderCreateRequest;
import com.justeam.justock_api.request.OrderUpdateRequest;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    private static final Set<String> ALLOWED_ORDER_STATUS = Set.of("EM ANDAMENTO", "CANCELADO", "CONCLUÍDO");
    private static final Set<String> ALLOWED_PAYMENT_STATUS = Set.of("PROCESSADO", "EM PROCESSAMENTO", "CANCELADO", "NEGADO");

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> listAllOrders() {
        return orderRepository.findAll();
    }

    public Order findOrder(int id) {
        return orderRepository.findById(id).orElse(null);
    }

    public Order createOrder(OrderCreateRequest dto) {
        validateOrderDates(dto.getDataEmissao(), dto.getDataEntrega());
        String normalizedStatus = normalizeOrderStatus(dto.getStatusPedido());
        String normalizedPaymentStatus = normalizePaymentStatus(dto.getStatusPagamento());

        Order order = new Order();
        order.setIdPedidoMarketplace(dto.getIdPedidoMarketplace());
        order.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
        order.setDataEntrega(dto.getDataEntrega());
        order.setDataEmissao(dto.getDataEmissao());
        order.setStatusPagamento(normalizedPaymentStatus);
        order.setStatusPedido(normalizedStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(int id, OrderUpdateRequest dto) {
        validateOrderDates(dto.getDataEmissao(), dto.getDataEntrega());
        String normalizedStatus = normalizeOrderStatus(dto.getStatusPedido());
        String normalizedPaymentStatus = normalizePaymentStatus(dto.getStatusPagamento());

        Order existingOrder = orderRepository.findById(id).orElse(null);
        if (existingOrder != null) {
            existingOrder.setIdPedidoMarketplace(dto.getIdPedidoMarketplace());
            existingOrder.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
            existingOrder.setDataEntrega(dto.getDataEntrega());
            existingOrder.setDataEmissao(dto.getDataEmissao());
            existingOrder.setStatusPagamento(normalizedPaymentStatus);
            existingOrder.setStatusPedido(normalizedStatus);
            return orderRepository.save(existingOrder);
        }
        return null;
    }

    public boolean deleteOrder(int id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
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
}
