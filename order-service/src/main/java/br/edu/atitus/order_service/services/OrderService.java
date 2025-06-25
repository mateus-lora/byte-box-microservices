package br.edu.atitus.order_service.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.atitus.order_service.clients.CurrencyClient;
import br.edu.atitus.order_service.clients.CurrencyResponse;
import br.edu.atitus.order_service.clients.ProductClient;
import br.edu.atitus.order_service.clients.ProductResponse;
import br.edu.atitus.order_service.entities.OrderEntity;
import br.edu.atitus.order_service.entities.OrderItemEntity;
import br.edu.atitus.order_service.repositories.OrderRepository;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final ProductClient productClient;
	private final CurrencyClient currencyClient;

	public OrderService(OrderRepository orderRepository, ProductClient productClient, CurrencyClient currencyClient) {
		this.orderRepository = orderRepository;
		this.productClient = productClient;
		this.currencyClient = currencyClient;
	}

	public OrderEntity createOrder(OrderEntity order, Long userId) {

		return orderRepository.save(order);
	}

	private Page<OrderEntity> processOrderPrices(Page<OrderEntity> orders, String targetCurrency) {
		for (OrderEntity order : orders) {
			double totalPrice = 0.0;
			double totalConvertedPrice = 0.0;

			for (OrderItemEntity item : order.getItems()) {
				ProductResponse product = productClient.getProductById(item.getProductId());
				item.setProduct(product);
				totalPrice += item.getPriceAtPurchase() * item.getQuantity();

				CurrencyResponse currencyResponse = currencyClient.getCurrency(item.getPriceAtPurchase(),
						item.getCurrencyAtPurchase(), targetCurrency);
				item.setConvertedPriceAtPruchase(currencyResponse.getConvertedValue());
				totalConvertedPrice += item.getConvertedPriceAtPruchase() * item.getQuantity();
			}
			order.setTotalPrice(totalPrice);
			order.setTotalConvertedPrice(totalConvertedPrice);
		}
		return orders;
	}

	public Page<OrderEntity> findOrdersByCustomerId(Long customerId, String targetCurrency, Pageable pageable) {
		Page<OrderEntity> orders = orderRepository.findByCustomerId(customerId, pageable);
		return processOrderPrices(orders, targetCurrency);
	}

	public Page<OrderEntity> findAll(String targetCurrency, Pageable pageable) {
		Page<OrderEntity> orders = orderRepository.findAll(pageable);
		return processOrderPrices(orders, targetCurrency);
	}
}
