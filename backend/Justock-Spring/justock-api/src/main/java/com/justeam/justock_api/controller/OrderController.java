package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.OrderResponseDTO;
import com.justeam.justock_api.model.Order;
import com.justeam.justock_api.request.OrderCreateRequest;
import com.justeam.justock_api.request.OrderUpdateRequest;
import com.justeam.justock_api.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/Order")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // GET /api/Order
    @GetMapping("/")
    public ApiResponseDTO<List<OrderResponseDTO>> index() {
        List<OrderResponseDTO> orders = orderService.listAllOrders()
                .stream()
                .map(o -> new OrderResponseDTO(o.getIdPedido(), o.getIdPedidoMarketplace(), o.getUsuarioMarketplaceId(), o.getDataEntrega(), o.getDataEmissao(), o.getStatusPagamento(), o.getStatusPedido()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "Orders encontrados!", orders);
    }

    // GET /api/Order/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    public ApiResponseDTO<OrderResponseDTO> show(@PathVariable int id) {
        Order order = orderService.findOrder(id);
        if (order == null) {
            return new ApiResponseDTO<>(404, "Order não encontrado!", null);
        }
        OrderResponseDTO dto = new OrderResponseDTO(order.getIdPedido(), order.getIdPedidoMarketplace(), order.getUsuarioMarketplaceId(), order.getDataEntrega(), order.getDataEmissao(), order.getStatusPagamento(), order.getStatusPedido());
        return new ApiResponseDTO<>(200, "Order encontrado!", dto);
    }

    // POST /api/Order/cadastrar
    @PostMapping("/cadastrar")
    public ApiResponseDTO<OrderResponseDTO> store(@Valid @RequestBody OrderCreateRequest request) {
        Order order = orderService.createOrder(request);
        OrderResponseDTO dto = new OrderResponseDTO(order.getIdPedido(), order.getIdPedidoMarketplace(), order.getUsuarioMarketplaceId(), order.getDataEntrega(), order.getDataEmissao(), order.getStatusPagamento(), order.getStatusPedido());
        return new ApiResponseDTO<>(200, "Order cadastrado com sucesso!", dto);
    }

    // PUT /api/Order/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    public ApiResponseDTO<OrderResponseDTO> update(@PathVariable int id, @Valid @RequestBody OrderUpdateRequest request) {
        Order order = orderService.updateOrder(id, request);
        if (order == null) {
            return new ApiResponseDTO<>(404, "Order não encontrado!", null);
        }
        OrderResponseDTO dto = new OrderResponseDTO(order.getIdPedido(), order.getIdPedidoMarketplace(), order.getUsuarioMarketplaceId(), order.getDataEntrega(), order.getDataEmissao(), order.getStatusPagamento(), order.getStatusPedido());
        return new ApiResponseDTO<>(200, "Order atualizado!", dto);
    }

    // DELETE /api/Order/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    public ApiResponseDTO<Void> destroy(@PathVariable int id) {
        boolean deleted = orderService.deleteOrder(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "Order não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "Order deletado!", null);
    }
}
