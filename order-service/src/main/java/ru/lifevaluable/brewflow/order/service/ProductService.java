package ru.lifevaluable.brewflow.order.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.lifevaluable.brewflow.order.dto.ProductResponse;
import ru.lifevaluable.brewflow.order.entity.Product;
import ru.lifevaluable.brewflow.order.exception.ProductNotFoundException;
import ru.lifevaluable.brewflow.order.mapper.ProductMapper;
import ru.lifevaluable.brewflow.order.repository.ProductRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Cacheable(value = "allProducts")
    public List<ProductResponse> getAllProducts() {
        log.debug("Getting all products");
        List<Product> products = productRepository.findAll();
        log.info("Retrieved {} products", products.size());
        return products.stream()
                .map(productMapper::toDTO)
                .toList();

    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(UUID id) {
        log.debug("Get product by id {}", id);
        if (id == null)
            throw new IllegalArgumentException("Product id can not be null");
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        log.info("Retrieved product: productId={}", id);
        return productMapper.toDTO(product);
    }
}
