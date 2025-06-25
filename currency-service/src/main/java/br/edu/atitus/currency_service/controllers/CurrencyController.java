package br.edu.atitus.currency_service.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.atitus.currency_service.clients.CurrencyBCClient;
import br.edu.atitus.currency_service.clients.CurrencyBCResponse;
import br.edu.atitus.currency_service.entities.CurrencyEntity;
import br.edu.atitus.currency_service.repositories.CurrencyRepository;

@RestController
@RequestMapping ("currency")
public class CurrencyController {

	private final CurrencyRepository repository;
	private final CurrencyBCClient currencyBCClient;
	private final CacheManager cacheManager;

	public CurrencyController(CurrencyRepository repository, CurrencyBCClient currencyBCClient, CacheManager cacheManager) {
		super();
		this.repository = repository;
		this.currencyBCClient = currencyBCClient;
		this.cacheManager = cacheManager;
	}
	
	@Value ("${server.port}")
	private int serverPort;
	
	@GetMapping ("/{value}/{source}/{target}")
	public ResponseEntity <CurrencyEntity> getCurrency(
			@PathVariable double value,
			@PathVariable String source,
			@PathVariable String target
			) throws Exception{
		
		source = source.toUpperCase();
		target = target.toUpperCase();
		String dataSource = "None";
		String nameCache = "Currency";
		String keyCache = source + target;
		
		CurrencyEntity currency = cacheManager.getCache(nameCache).get(keyCache, CurrencyEntity.class);
		
		if (currency != null) {
			dataSource = "Cache";
		} else {
			currency = new CurrencyEntity();
			currency.setSource(source);
			currency.setTarget(target);
			
			if (source.equals(target)) {
				currency.setConversionRate(1);
			} else {
				try {
					double currencySource = 1;
					double currencyTarget = 1;
					if (!source.equals("BRL")) {
						CurrencyBCResponse response = currencyBCClient.getCurrency(source);
						if (response.getValue().isEmpty()) throw new Exception("Currency not found for " + source);
						currencySource = response.getValue().get(0).getCotacaoVenda();
					}
					if (!target.equals("BRL")) {
						CurrencyBCResponse response = currencyBCClient.getCurrency(target);
						if (response.getValue().isEmpty()) throw new Exception("Currency not found for " + target);
						currencyTarget = response.getValue().get(0).getCotacaoVenda();
					}
					currency.setConversionRate(currencySource / currencyTarget);
					dataSource = "API BCB";
				} catch (Exception e) {
					currency = repository.findBySourceAndTarget(source, target)
							.orElseThrow(() -> new Exception("Currency Unsupported"));
					dataSource = "Local Database";
				}
			}
			
			cacheManager.getCache(nameCache).put(keyCache, currency);

		}
				
		currency.setConvertedValue(value * currency.getConversionRate());
		currency.setEnviroment("Currency-service running on port: " + serverPort + " - DataSource: " + dataSource);
		
		return ResponseEntity.ok(currency);
	}
	
}