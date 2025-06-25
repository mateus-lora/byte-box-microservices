package br.edu.atitus.order_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/products/noconverter/{id}")
    ProductResponse getProductById(@PathVariable Long id);
    
    @GetMapping("/products/{id}/{targetCurrency}")
    ProductResponse getProductByIdWithCurrency(@PathVariable Long id, @PathVariable String targetCurrency);
    
    @PutMapping("/products/{productId}/{decreaseStock}")
    void decreaseStock(@PathVariable Long productId, @PathVariable int decreaseStock);

}