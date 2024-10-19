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
import io.github.resilience4j.retry.annotation.Retry; // Resilience4j
// No importamos reactor.util.retry.Retry // se hizo directamente

import reactor.core.publisher.Mono;

/**
 * EnrichmentServiceImpl: Implementación de la interfaz EnrichmentService que se
 * encarga de realizar las llamadas HTTP a las APIs externas (implementadas en Go) para
 * obtener datos enriquecidos de clientes y productos.
 * Responsabilidades:
 * - Realizar solicitudes HTTP a las APIs de Go para obtener los detalles de
 * clientes y productos.
 * - Manejar reintentos automáticos en caso de errores en las llamadas, utilizando Resilience4j.
 * - Proveer métodos de fallback en caso de que las solicitudes fallen después de varios reintentos.
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-18
 */
@Service
public class EnrichmentServiceImpl implements EnrichmentService {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor que inicializa el WebClient para realizar llamadas HTTP.
     */
    public EnrichmentServiceImpl(WebClient.Builder webClientBuilder,
                                 @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
        this.redisTemplate = redisTemplate;
    }
    /**
     * enrichCustomer: Realiza una solicitud HTTP a la API de Go para enriquecer los
     * datos del cliente, incluyendo un mecanismo de reintento en caso de fallo.
     * @param orderMessage El mensaje del pedido que contiene el ID del cliente.
     * @return Mono<CustomerDetails> Un flujo reactivo que contiene los detalles del  cliente.
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
                        return Mono.just(customer); // Devuelve el cliente cacheado
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error al deserializar cliente cacheado"));
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
                        .println("Error al enriquecer los datos del cliente: " + error.getMessage()));
    }

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
                        .println("Error al enriquecer los datos del cliente con Reactor: " + error.getMessage()));
    }

    /**
     * enrichProduct: Realiza una solicitud HTTP a la API de Go para enriquecer los
     * datos del producto, incluyendo un mecanismo de reintento en caso de fallo.
     * @param orderMessage El mensaje del pedido que contiene los detalles del
     * @return Mono<ProductDetails> Un flujo reactivo que contiene los detalles del producto.
     */
    @Override
    @Retry(name = "productRetry", fallbackMethod = "fallbackProduct")
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackProduct")
    public Mono<ProductDetails> enrichProductWithResilience(OrderMessage orderMessage) {
        String productCacheKey = "product:" + orderMessage.getProducts().get(0).getProductId();

        // Intentar obtener el producto del caché (Redis)
        return redisTemplate.opsForValue().get(productCacheKey)
                .flatMap(cachedProduct -> {
                    // Si el producto está en caché, deserializarlo y retornarlo
                    try {
                        return Mono.just(objectMapper.readValue(cachedProduct, ProductDetails.class));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Error al deserializar el producto desde caché."));
                    }
                })
                .switchIfEmpty(
                        // Si el producto no está en caché, realizar la llamada a la API externa
                        webClient.get()
                                .uri("/product")
                                .retrieve()
                                .bodyToMono(ProductDetails.class)
                                .flatMap(product -> {
                                    // Almacenar el producto en caché después de obtenerlo de la API
                                    return Mono.fromCallable(() -> objectMapper.writeValueAsString(product))
                                            .flatMap(serializedProduct -> redisTemplate.opsForValue()
                                                    .set(productCacheKey, serializedProduct))
                                            .thenReturn(product); // Retornar el producto después de almacenarlo en
                                                                  // caché
                                })
                                .doOnError(error -> {
                                    System.err.println(
                                            "Error al enriquecer los datos del producto: " + error.getMessage());
                                }));
    }

    // Método de fallback en caso de que los reintentos fallen para clientes
    public Mono<CustomerDetails> fallbackCustomer(OrderMessage orderMessage, Throwable t) {
        System.err.println("Fallo en el enriquecimiento de cliente después de reintentos: " + t.getMessage());
        return Mono.error(new RuntimeException("No se pudo enriquecer el cliente después de varios intentos."));
    }

    // Método de fallback en caso de que los reintentos fallen para productos
    public Mono<ProductDetails> fallbackProduct(OrderMessage orderMessage, Throwable t) {
        System.err.println("Fallo en el enriquecimiento de producto después de reintentos: " + t.getMessage());
        return Mono.error(new RuntimeException("No se pudo enriquecer el producto después de varios intentos."));
    }
}
