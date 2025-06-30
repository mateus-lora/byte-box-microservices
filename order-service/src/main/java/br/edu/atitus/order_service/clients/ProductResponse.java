package br.edu.atitus.order_service.clients;

public record ProductResponse(
		String theme,
		Integer quantity,
		String description,
		String category,
		double price,
		String currency,
		Integer stock,
		String imageUrl){
	
}
