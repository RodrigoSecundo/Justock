package com.justeam.justock_api.service;

import java.time.LocalDate;
import java.util.List;

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

        Order order = new Order();
        order.setIdPedidoMarketplace(dto.getIdPedidoMarketplace());
        order.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
        order.setDataEntrega(dto.getDataEntrega());
        order.setDataEmissao(dto.getDataEmissao());
        order.setStatusPagamento(dto.getStatusPagamento());
        order.setStatusPedido(dto.getStatusPedido());
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(int id, OrderUpdateRequest dto) {
        validateOrderDates(dto.getDataEmissao(), dto.getDataEntrega());

        Order existingOrder = orderRepository.findById(id).orElse(null);
        if (existingOrder != null) {
            existingOrder.setIdPedidoMarketplace(dto.getIdPedidoMarketplace());
            existingOrder.setUsuarioMarketplaceId(dto.getUsuarioMarketplaceId());
            existingOrder.setDataEntrega(dto.getDataEntrega());
            existingOrder.setDataEmissao(dto.getDataEmissao());
            existingOrder.setStatusPagamento(dto.getStatusPagamento());
            existingOrder.setStatusPedido(dto.getStatusPedido());
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

        if (dataEntrega != null && dataEntrega.isBefore(dataEmissao)) {
            throw new BadRequestException("A data de entrega não pode ser anterior à data de emissão.");
        }
    }
}
