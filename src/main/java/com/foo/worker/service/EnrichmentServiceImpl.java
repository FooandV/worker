package com.foo.worker.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import java.time.Duration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import reactor.core.publisher.Mono;

/**
 * EnrichmentServiceImpl: Implementation of the EnrichmentService interface responsible for making HTTP calls
 * to external APIs (developed in Go) in order to enrich orders with detailed customer and product data.
 * 
 * Responsibilities:
 * - Perform HTTP requests to Go APIs to retrieve customer and product information.
 * - Handle automatic retries using Resilience4j in case of request failures.
 * - Provide fallback methods when retry attempts are exhausted.
 * - Cache enriched responses in Redis to improve performance.
 * 
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-18
 */
@Service
public class EnrichmentServiceImpl implements EnrichmentService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EnrichmentServiceImpl(WebClient.Builder webClientBuilder,
                                 @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
        this.redisTemplate = redisTemplate;
    }

    /**
     * Enriches customer data by calling the Go-based API and applying retry mechanisms.
     * If the data is found in Redis, it is returned from the cache.
     *
     * @param orderMessage The order message containing the customer ID.
     * @return Mono<CustomerDetails> with enriched customer data.
     */
    @Override
    @Retry(name = "customerRetry", fallbackMethod = "fallbackCustomer")
    @CircuitBreaker(name = "customerService", fallbackMethod = "fallbackCustomer")
    public Mono<CustomerDetails> enrichCustomerWithResilience(OrderMessage orderMessage) {
        String customerCacheKey = "customer:" + orderMessage.getCustomerId();
        return redisTemplate.opsForValue().get(customerCacheKey)
                .flatMap(cachedCustomer -> {
                    try {
                        CustomerDetails customer = objectMapper.readValue(cachedCustomer, CustomerDetails.class);
                        return Mono.just(customer); // Return cached customer
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error deserializing cached customer"));
                    }
                })
                .switchIfEmpty(
                        webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/customer")
                                        .queryParam("customerId", orderMessage.getCustomerId())
                                        .build())
                                .retrieve()
                                .bodyToMono(CustomerDetails.class)
                                .flatMap(customer -> Mono.fromCallable(() -> objectMapper.writeValueAsString(customer)) 
                                        .flatMap(serializedCustomer -> redisTemplate.opsForValue()
                                                .set(customerCacheKey, serializedCustomer)
                                                .thenReturn(customer))))
                .doOnError(error -> System.err
                        .println("Error enriching customer data: " + error.getMessage()));
    }

    /**
     * Enriches customer data using Reactor's retry strategy (without Resilience4j).
     *
     * @param orderMessage The order message containing the customer ID.
     * @return Mono<CustomerDetails> with enriched data.
     */
    @Override
    public Mono<CustomerDetails> enrichCustomerWithReactor(OrderMessage orderMessage) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/customer")
                        .queryParam("customerId", orderMessage.getCustomerId())
                        .build())
                .retrieve()
                .bodyToMono(CustomerDetails.class)
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .jitter(0.5))
                .doOnError(error -> System.err
                        .println("Error enriching customer data with Reactor: " + error.getMessage()));
    }

    /**
     * Enriches product data from the Go-based API and caches it in Redis.
     * 
     * @param orderMessage The order message containing the product ID.
     * @return Mono<ProductDetails> with enriched product data.
     */
    @Override
    @Retry(name = "productRetry", fallbackMethod = "fallbackProduct")
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackProduct")
    public Mono<ProductDetails> enrichProductWithResilience(OrderMessage orderMessage) {
        String productCacheKey = "product:" + orderMessage.getProducts().get(0).getProductId();

        return redisTemplate.opsForValue().get(productCacheKey)
                .flatMap(cachedProduct -> {
                    try {
                        return Mono.just(objectMapper.readValue(cachedProduct, ProductDetails.class));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Error deserializing cached product"));
                    }
                })
                .switchIfEmpty(
                        webClient.get()
                                .uri("/product")
                                .retrieve()
                                .bodyToMono(ProductDetails.class)
                                .flatMap(product ->
                                        Mono.fromCallable(() -> objectMapper.writeValueAsString(product))
                                                .flatMap(serializedProduct -> redisTemplate.opsForValue()
                                                        .set(productCacheKey, serializedProduct))
                                                .thenReturn(product))
                                .doOnError(error -> {
                                    System.err.println("Error enriching product data: " + error.getMessage());
                                }));
    }

    /**
     * Fallback method for customer enrichment in case retries fail.
     */
    public Mono<CustomerDetails> fallbackCustomer(Order
