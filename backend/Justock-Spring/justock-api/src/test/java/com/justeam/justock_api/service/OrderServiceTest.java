package com.justeam.justock_api.service;

import com.justeam.justock_api.model.Order;
import com.justeam.justock_api.model.OrderItem;
import com.justeam.justock_api.model.Product;
import com.justeam.justock_api.repository.OrderItemRepository;
import com.justeam.justock_api.repository.OrderRepository;
import com.justeam.justock_api.repository.ProductRepository;
import com.justeam.justock_api.repository.UserMarketplaceRepository;
import com.justeam.justock_api.request.OrderCreateRequest;
import com.justeam.justock_api.request.OrderItemRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserMarketplaceRepository userMarketplaceRepository;

    @Mock
    private DashboardEventService dashboardEventService;

    @Mock
    private MercadoLivreService mercadoLivreService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderAllowsSameProductInDifferentManualOrders() {
        Product product = new Product();
        product.setIdProduto(42);
        product.setUsuario(1);
        product.setCategoria("GPU");
        product.setMarca("NVIDIA");
        product.setNomeDoProduto("RTX 3060");
        product.setEstado("ATIVO");
        product.setPreco(BigDecimal.valueOf(1999.90));
        product.setCodigoDeBarras("123");
        product.setQuantidade(5);
        product.setQuantidadeReservada(0);
        product.setMarcador("MANUAL");

        AtomicInteger nextOrderId = new AtomicInteger(100);
        List<OrderItem> persistedItems = new ArrayList<>();

        when(productRepository.findById(42)).thenReturn(Optional.of(product));
        when(productRepository.decrementQuantityIfEnough(42, 1)).thenAnswer(invocation -> {
            int currentQuantity = product.getQuantidade();
            if (currentQuantity < 1) {
                return 0;
            }
            product.setQuantidade(currentQuantity - 1);
            return 1;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getIdPedido() == 0) {
                order.setIdPedido(nextOrderId.getAndIncrement());
            }
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            persistedItems.add(item);
            return item;
        });
        when(orderItemRepository.findByIdIdPedido(any(Integer.class))).thenAnswer(invocation -> {
            Integer orderId = invocation.getArgument(0);
            return persistedItems.stream()
                    .filter(item -> item.getId() != null && orderId.equals(item.getId().getIdPedido()))
                    .toList();
        });

        Order firstOrder = orderService.createOrder(buildManualOrderRequest(42), 1);
        Order secondOrder = orderService.createOrder(buildManualOrderRequest(42), 1);

        assertEquals(100, firstOrder.getIdPedido());
        assertEquals(101, secondOrder.getIdPedido());
        assertEquals(3, product.getQuantidade());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(2)).save(itemCaptor.capture());
        List<OrderItem> savedItems = itemCaptor.getAllValues();
        assertEquals(100, savedItems.get(0).getId().getIdPedido());
        assertEquals(101, savedItems.get(1).getId().getIdPedido());
        assertEquals(42, savedItems.get(0).getId().getIdProduto());
        assertEquals(42, savedItems.get(1).getId().getIdProduto());
    }

    @Test
    void createOrderFailsWhenAtomicStockUpdateCannotReserveLastUnit() {
        Product product = new Product();
        product.setIdProduto(42);
        product.setUsuario(1);
        product.setCategoria("GPU");
        product.setMarca("NVIDIA");
        product.setNomeDoProduto("RTX 3060");
        product.setEstado("ATIVO");
        product.setPreco(BigDecimal.valueOf(1999.90));
        product.setCodigoDeBarras("123");
        product.setQuantidade(1);
        product.setQuantidadeReservada(0);
        product.setMarcador("MANUAL");

        AtomicInteger nextOrderId = new AtomicInteger(100);
        List<OrderItem> persistedItems = new ArrayList<>();

        when(productRepository.findById(42)).thenReturn(Optional.of(product));
        when(productRepository.decrementQuantityIfEnough(42, 1)).thenReturn(0);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getIdPedido() == 0) {
                order.setIdPedido(nextOrderId.getAndIncrement());
            }
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            persistedItems.add(item);
            return item;
        });
        when(orderItemRepository.findByIdIdPedido(any(Integer.class))).thenAnswer(invocation -> {
            Integer orderId = invocation.getArgument(0);
            return persistedItems.stream()
                    .filter(item -> item.getId() != null && orderId.equals(item.getId().getIdPedido()))
                    .toList();
        });

        assertThrows(RuntimeException.class, () -> orderService.createOrder(buildManualOrderRequest(42), 1));
    }

    private OrderCreateRequest buildManualOrderRequest(Integer productId) {
        OrderItemRequest item = new OrderItemRequest();
        item.setIdProduto(productId);
        item.setQuantidade(1);
        item.setPrecoUnitario(BigDecimal.valueOf(1999.90));

        OrderCreateRequest request = new OrderCreateRequest();
        request.setIdPedidoMarketplace(4);
        request.setDataEmissao(LocalDate.now());
        request.setStatusPagamento("EM PROCESSAMENTO");
        request.setStatusPedido("EM ANDAMENTO");
        request.setItens(List.of(item));
        return request;
    }
}