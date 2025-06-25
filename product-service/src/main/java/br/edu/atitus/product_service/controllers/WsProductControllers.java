package br.edu.atitus.product_service.controllers;

import javax.security.sasl.AuthenticationException;

import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.atitus.product_service.dtos.ProductDTO;
import br.edu.atitus.product_service.entities.ProductEntity;
import br.edu.atitus.product_service.repositories.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/ws/products")
public class WsProductControllers {

	private final ProductRepository repository;

	public WsProductControllers(ProductRepository repository) {
		super();
		this.repository = repository;
	}
	
	private ProductEntity converteDto2Entity(ProductDTO dto) {
		var product = new ProductEntity();
		BeanUtils.copyProperties(dto, product);
		return product;
	}
	
	@Operation(description = "Cadastrar Produto")
	@PostMapping
	public ResponseEntity<ProductEntity> createProduct(
			@RequestBody ProductDTO dto,
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Email") String userEmail,
			@RequestHeader("X-User-Type") Integer userType) throws Exception {
		//Somente Admin
		if (userType != 0)
			throw new AuthenticationException("Usuário sem permissão");
		
		var product = converteDto2Entity(dto);
		repository.save(product);
		
		return ResponseEntity.status(201).body(product);
	}
	
	@Operation(description = "Editar Produto")
	@PutMapping("/{idProduct}")
	public ResponseEntity<ProductEntity> updateProduct(
			@PathVariable Long idProduct,
			@RequestBody ProductDTO dto,
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Email") String userEmail,
			@RequestHeader("X-User-Type") Integer userType) throws Exception {
		//Somente Admin
		if (userType != 0)
			throw new AuthenticationException("Usuário sem permissão");
		
		var product = converteDto2Entity(dto);
		product.setId(idProduct);
		repository.save(product);
		
		return ResponseEntity.ok(product);
	}
	
	@Operation(description = "Deletar Produto")
	@DeleteMapping("/{idProduct}")
	public ResponseEntity<String> deleteProduct(
			@PathVariable Long idProduct,
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Email") String userEmail,
			@RequestHeader("X-User-Type") Integer userType) throws Exception {
		//Somente Admin
		if (userType != 0)
			throw new AuthenticationException("Usuário sem permissão");
		
		repository.deleteById(idProduct);
		
		return ResponseEntity.ok("Excluído");
	}
	
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<String> handlerAuth(AuthenticationException e) {
		String message = e.getMessage().replaceAll("[\\r\\n]", "");
		return ResponseEntity.status(403).body(message);
	};
	
}
