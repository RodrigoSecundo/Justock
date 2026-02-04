package com.justeam.justock_api.controller;

import com.justeam.justock_api.dto.ApiResponseDTO;
import com.justeam.justock_api.dto.StockOrderResponseDTO;
import com.justeam.justock_api.model.StockOrder;
import com.justeam.justock_api.request.StockOrderCreateRequest;
import com.justeam.justock_api.request.StockOrderUpdateRequest;
import com.justeam.justock_api.service.StockOrderService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/StockOrder")
@CrossOrigin(origins = "*")
public class StockOrderController {

    @Autowired
    private StockOrderService stockOrderService;

    // GET /api/StockOrder
    @GetMapping("/")
    public ApiResponseDTO<List<StockOrderResponseDTO>> index() {
        List<StockOrderResponseDTO> stockOrders = stockOrderService.listAllStockOrders()
                .stream()
                .map(so -> new StockOrderResponseDTO(so.getIdProduto(), so.getIdPedido(), so.getQuantidade(), so.getPrecoUnitario(), so.getSubtotal(), so.getIdItemMarketplace(), so.getItemStatus()))
                .collect(Collectors.toList());
        return new ApiResponseDTO<>(200, "StockOrders encontrados!", stockOrders);
    }

    // GET /api/StockOrder/visualizar/{id}
    @GetMapping("/visualizar/{id}")
    public ApiResponseDTO<StockOrderResponseDTO> show(@PathVariable int id) {
        StockOrder stockOrder = stockOrderService.findStockOrder(id);
        if (stockOrder == null) {
            return new ApiResponseDTO<>(404, "StockOrder não encontrado!", null);
        }
        StockOrderResponseDTO dto = new StockOrderResponseDTO(stockOrder.getIdProduto(), stockOrder.getIdPedido(), stockOrder.getQuantidade(), stockOrder.getPrecoUnitario(), stockOrder.getSubtotal(), stockOrder.getIdItemMarketplace(), stockOrder.getItemStatus());
        return new ApiResponseDTO<>(200, "StockOrder encontrado!", dto);
    }

    // POST /api/StockOrder/cadastrar
    @PostMapping("/cadastrar")
    public ApiResponseDTO<StockOrderResponseDTO> store(@Valid @RequestBody StockOrderCreateRequest request) {
        StockOrder stockOrder = stockOrderService.createStockOrder(request);
        StockOrderResponseDTO dto = new StockOrderResponseDTO(stockOrder.getIdProduto(), stockOrder.getIdPedido(), stockOrder.getQuantidade(), stockOrder.getPrecoUnitario(), stockOrder.getSubtotal(), stockOrder.getIdItemMarketplace(), stockOrder.getItemStatus());
        return new ApiResponseDTO<>(200, "StockOrder cadastrado com sucesso!", dto);
    }

    // PUT /api/StockOrder/atualizar/{id}
    @PutMapping("/atualizar/{id}")
    public ApiResponseDTO<StockOrderResponseDTO> update(@PathVariable int id, @Valid @RequestBody StockOrderUpdateRequest request) {
        StockOrder stockOrder = stockOrderService.updateStockOrder(id, request);
        if (stockOrder == null) {
            return new ApiResponseDTO<>(404, "StockOrder não encontrado!", null);
        }
        StockOrderResponseDTO dto = new StockOrderResponseDTO(stockOrder.getIdProduto(), stockOrder.getIdPedido(), stockOrder.getQuantidade(), stockOrder.getPrecoUnitario(), stockOrder.getSubtotal(), stockOrder.getIdItemMarketplace(), stockOrder.getItemStatus());
        return new ApiResponseDTO<>(200, "StockOrder atualizado!", dto);
    }

    // DELETE /api/StockOrder/deletar/{id}
    @DeleteMapping("/deletar/{id}")
    public ApiResponseDTO<Void> destroy(@PathVariable int id) {
        boolean deleted = stockOrderService.deleteStockOrder(id);
        if (!deleted) {
            return new ApiResponseDTO<>(404, "StockOrder não encontrado!", null);
        }
        return new ApiResponseDTO<>(200, "StockOrder deletado!", null);
    }
}
