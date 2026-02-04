package com.justeam.justock_api.service;

import com.justeam.justock_api.model.StockOrder;
import com.justeam.justock_api.repository.StockOrderRepository;
import com.justeam.justock_api.request.StockOrderCreateRequest;
import com.justeam.justock_api.request.StockOrderUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class StockOrderService {

    @Autowired
    private StockOrderRepository stockOrderRepository;

    public List<StockOrder> listAllStockOrders() {
        return stockOrderRepository.findAll();
    }

    public StockOrder findStockOrder(int id) {
        return stockOrderRepository.findById(id).orElse(null);
    }

    public StockOrder createStockOrder(StockOrderCreateRequest dto) {
        StockOrder stockOrder = new StockOrder();
        stockOrder.setIdProduto(dto.getIdProduto());
        stockOrder.setIdPedido(dto.getIdPedido());
        stockOrder.setQuantidade(dto.getQuantidade());
        stockOrder.setPrecoUnitario(dto.getPrecoUnitario());
        stockOrder.setSubtotal(dto.getSubtotal());
        stockOrder.setIdItemMarketplace(dto.getIdItemMarketplace());
        stockOrder.setItemStatus(dto.getItemStatus());
        return stockOrderRepository.save(stockOrder);
    }

    @Transactional
    public StockOrder updateStockOrder(int id, StockOrderUpdateRequest dto) {
        StockOrder existingStockOrder = stockOrderRepository.findById(id).orElse(null);
        if (existingStockOrder != null) {
            existingStockOrder.setIdPedido(dto.getIdPedido());
            existingStockOrder.setQuantidade(dto.getQuantidade());
            existingStockOrder.setPrecoUnitario(dto.getPrecoUnitario());
            existingStockOrder.setSubtotal(dto.getSubtotal());
            existingStockOrder.setIdItemMarketplace(dto.getIdItemMarketplace());
            existingStockOrder.setItemStatus(dto.getItemStatus());
            return stockOrderRepository.save(existingStockOrder);
        }
        return null;
    }

    public boolean deleteStockOrder(int id) {
        if (stockOrderRepository.existsById(id)) {
            stockOrderRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
