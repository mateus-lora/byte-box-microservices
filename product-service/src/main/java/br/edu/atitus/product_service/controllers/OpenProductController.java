package br.edu.atitus.product_service.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.edu.atitus.product_service.clients.CurrencyClient;
import br.edu.atitus.product_service.clients.CurrencyResponse;
import br.edu.atitus.product_service.entities.ProductEntity;
import br.edu.atitus.product_service.repositories.ProductRepository;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("products")
public class OpenProductController {

	private final ProductRepository repository;
	private final CurrencyClient currencyClient;
	private final CacheManager cacheManager;

	public OpenProductController(ProductRepository repository, CurrencyClient currencyClient,
			CacheManager cacheManager) {
		super();
		this.repository = repository;
		this.currencyClient = currencyClient;
		this.cacheManager = cacheManager;
	}

	@Value("${server.port}")
	private int serverPort;
	
    private void applyCurrencyConversion(ProductEntity product, String targetCurrency) {
        product.setEnvironment("Product-service running on Port: " + serverPort);

        if (product.getCurrency().equals(targetCurrency)) {
            product.setConvertedPrice(product.getPrice());
        } else {
            CurrencyResponse currency = currencyClient.getCurrency(product.getPrice(), product.getCurrency(),
                    targetCurrency);
            if (currency != null) {
                product.setConvertedPrice(currency.getConvertedValue());
                product.setEnvironment(product.getEnvironment() + " - " + currency.getEnviroment());
            } else {
                product.setConvertedPrice(-1);
                product.setEnvironment(product.getEnvironment() + " - Currency unavalaible");
            }
        }
    }

	@Operation(description = "Lista de Produtos")
    @GetMapping("/{targetCurrency}")
    public ResponseEntity<Page<ProductEntity>> getAllProducts(
    		@PathVariable String targetCurrency,
    		@PageableDefault(page = 0, size = 5, sort = "description", direction = Direction.ASC)
    		Pageable pageable) throws Exception {
    	//Page<ProductEntity> products = repository.findAll(pageable);
    	Page<ProductEntity> products = repository.findByStockGreaterThan(0, pageable);

    	for (ProductEntity product : products) {
            applyCurrencyConversion(product, targetCurrency);
    	}
    	return ResponseEntity.ok(products);
    }

	@Operation(description = "Buscar Produto por ID")
    @GetMapping("/{idProduct}/{targetCurrency}")
    public ResponseEntity<ProductEntity> getProduct(@PathVariable Long idProduct, @PathVariable String targetCurrency)
            throws Exception {

        targetCurrency = targetCurrency.toUpperCase();
        String nameCache = "Product";
        String keyCache = idProduct + "-" + targetCurrency;

        ProductEntity product = cacheManager.getCache(nameCache).get(keyCache, ProductEntity.class);

        if (product == null) {
            product = repository.findById(idProduct).orElseThrow(() -> new Exception("Product not found"));

            if (product.getStock() <= 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + idProduct + " is out of stock or not available.");
            }
            
            applyCurrencyConversion(product, targetCurrency);

            cacheManager.getCache(nameCache).put(keyCache, product);

        } else {
            product.setEnvironment("Product-service running on Port: " + serverPort + " - Datasource: cache");
        }

        return ResponseEntity.ok(product);
    }
    
	@Operation(description = "Pesquisar Produto")
    @GetMapping("/search/{contains}/{targetCurrency}")
    public ResponseEntity<List<ProductEntity>> searchProductsByTheme(
            @PathVariable String contains,
            @PathVariable String targetCurrency) {
        try {
            List<ProductEntity> products = repository.findByThemeContainingIgnoreCaseAndStockGreaterThan(contains, 0);

            if (products.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            
            products.forEach(product -> applyCurrencyConversion(product, targetCurrency.toUpperCase()));

            return ResponseEntity.ok(products);
        } catch (Exception e) {
            System.err.println("Erro ao buscar produtos pelo tema: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
	@Hidden
    @GetMapping("/noconverter/{idProduct}")
    public ResponseEntity<ProductEntity> getNoConverter(@PathVariable Long idProduct) throws Exception {
    	var product = repository.findById(idProduct).orElseThrow(() -> new Exception("Produto não encontrado"));
    	
    	product.setConvertedPrice(-1);
		product.setEnvironment("Product-service running on Port: " + serverPort);
		return ResponseEntity.ok(product);
    }
    
	@Hidden
    @PutMapping("/{productId}/{decreaseStock}")
    public ResponseEntity<Void> decreaseStock(@PathVariable Long productId, @PathVariable int decreaseStock) {
        ProductEntity product = repository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

        if (product.getStock() < decreaseStock) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estoque do producto insuficiente" + productId);
        }

        product.setStock(product.getStock() - decreaseStock);

        repository.save(product);

        return ResponseEntity.ok().build();
    }
    
}
