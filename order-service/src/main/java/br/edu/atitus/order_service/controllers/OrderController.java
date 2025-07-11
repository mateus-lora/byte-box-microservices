package br.edu.atitus.order_service.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.logging.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.edu.atitus.order_service.clients.ProductClient;
import br.edu.atitus.order_service.clients.ProductResponse;
import br.edu.atitus.order_service.dtos.OrderDTO;
import br.edu.atitus.order_service.entities.OrderEntity;
import br.edu.atitus.order_service.entities.OrderItemEntity;
import br.edu.atitus.order_service.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/ws/orders")
public class OrderController {

	private final OrderService orderService;
	private final ProductClient productClient;

	public OrderController(OrderService orderService, ProductClient productClient) {
		this.orderService = orderService;
		this.productClient = productClient;
	}

	@Operation(description = "Registrar Pedido")
	@PostMapping
	public ResponseEntity<OrderEntity> createOrder(
			@RequestBody OrderDTO orderDTO,
			@RequestHeader("X-User-Id") Long userId,
			 @RequestHeader("X-User-Email") String userEmail,
			 @RequestHeader("X-User-Type") Integer userType) {
		
		OrderEntity order = new OrderEntity();
        order.setOrderDate(LocalDateTime.now());
        order.setCustomerId(userId);
        
        List<OrderItemEntity> items = orderDTO.items().stream().map(dto -> {
            OrderItemEntity item = new OrderItemEntity();
            item.setProductId(dto.productId());
            item.setQuantity(dto.quantity());

            // Busca os dados do produto via FeignClient para obter preço, descrição, etc
            ProductResponse product = productClient.getProductById(dto.productId());
            item.setPriceAtPurchase(product.price());
            item.setCurrencyAtPurchase(product.currency());

            // Preenche o product transient para retorno
            item.setProduct(product);

            item.setOrder(order);
            
            // Diminuir estoque do produto
            productClient.decreaseStock(dto.productId(), dto.quantity());

            return item;
        }).toList();

        order.setItems(items);
		
		
		orderService.createOrder(order, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(order);
	}

	@Operation(description = "Listar Pedidos")
	@GetMapping("/{targetCurrency}")
	public ResponseEntity<Page<OrderEntity>> listOrdersByUser(
			@PathVariable String targetCurrency,
			@PageableDefault(page = 0,size = 5,sort = "orderDate", direction = Direction.ASC) 
				Pageable pageable,
			@RequestHeader("X-User-Id") Long userId,
			 @RequestHeader("X-User-Email") String userEmail,
			 @RequestHeader("X-User-Type")Integer userType) {
		targetCurrency = targetCurrency.toUpperCase();
        if (userType == 0) {
            return ResponseEntity.ok(orderService.findAll(targetCurrency, pageable));
        } else {
            return ResponseEntity.ok(orderService.findOrdersByCustomerId(userId, targetCurrency, pageable));
        }

	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception e) {
		String cleanMessage = e.getMessage().replaceAll("[\\r\\n]", " ");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cleanMessage);
	}

}
