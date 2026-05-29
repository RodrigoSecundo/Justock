package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.OrderItemResponseDTO;
import com.justeam.justock_api.dto.OrderResponseDTO;
import com.justeam.justock_api.model.Order;
import com.justeam.justock_api.request.OrderCreateRequest;
import com.justeam.justock_api.request.OrderUpdateRequest;
import com.justeam.justock_api.service.CurrentAccountService;
import com.justeam.justock_api.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/Order")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CurrentAccountService currentAccountService;

    // GET /api/Order
    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<OrderResponseDTO>> index() {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        boolean includeAllOrders = currentAccountService.isPrimaryAdmin();
        List<OrderResponseDTO> orders = orderService.listOrdersByUsuario(usuarioId, includeAllOrders)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "Orders encontrados!", orders);
    }

    // GET /api/Order/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<OrderResponseDTO> show(@PathVariable int id) {
        Integer usuarioId = currentAccountService.getDashboardUserId();
        boolean includeAllOrders = currentAccountService.isPrimaryAdmin();
        Order order = orderService.findOrderVisibleToUsuario(id, usuarioId, includeAllOrders);
        if (order == null) {
            return new ApiResponseDTO<>(404, "Order não encontrado!", null);
        }
        OrderResponseDTO dto = toDto(order);
        return new ApiResponseDTO<>(200, "Order encontrado!", dto);
    }

    // POST /api/Order/cadastrar
    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<OrderResponseDTO> store(@Valid @RequestBody OrderCreateRequest request) {
        Order order = orderService.createOrder(request, currentAccountService.getDashboardUserId());
        OrderResponseDTO dto = toDto(order);
        return new ApiResponseDTO<>(200, "Order cadastrado com sucesso!", dto);
    }

    // PUT /api/Order/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<OrderResponseDTO> update(@PathVariable int id, @Valid @RequestBody OrderUpdateRequest request) {
        Order order = orderService.updateOrder(id, request, currentAccountService.getDashboardUserId());
        if (order == null) {
            return new ApiResponseDTO<>(404, "Order não encontrado!", null);
        }
        OrderResponseDTO dto = toDto(order);
        return new ApiResponseDTO<>(200, "Order atualizado!", dto);
    }

    // DELETE /api/Order/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<Void> destroy(@PathVariable int id) {
        boolean deleted = orderService.deleteOrder(id, currentAccountService.getDashboardUserId());
        if (!deleted) {
            return new ApiResponseDTO<>(404, "Order não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "Order deletado!", null);
    }

    private OrderResponseDTO toDto(Order order) {
        List<OrderItemResponseDTO> itens = orderService.listOrderItemResponses(order.getIdPedido());
        return new OrderResponseDTO(
                order.getIdPedido(),
                order.getIdPedidoMarketplace(),
                order.getUsuarioMarketplaceId(),
                order.getDataEntrega(),
                order.getDataEmissao(),
                order.getStatusPagamento(),
                order.getStatusPedido(),
                order.getMarketplaceResourceId(),
                order.getMarketplaceSource(),
                order.getObservacao(),
                order.getInventoryApplied(),
                orderService.calculateOrderTotal(order.getIdPedido()),
                itens);
    }
}
