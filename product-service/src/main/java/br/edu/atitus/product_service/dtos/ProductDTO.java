package br.edu.atitus.product_service.dtos;

public record ProductDTO(
		String theme,
		Integer quantity,
		String description,
		String category,
		double price,
		String currency,
		Integer stock,
		String imageUrl) {

}
