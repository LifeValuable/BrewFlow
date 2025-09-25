package ru.lifevaluable.brewflow.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.order.dto.ProductResponse;
import ru.lifevaluable.brewflow.order.entity.Product;
import ru.lifevaluable.brewflow.order.exception.ProductNotFoundException;
import ru.lifevaluable.brewflow.order.mapper.ProductMapper;
import ru.lifevaluable.brewflow.order.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toDTO)
                .toList();
    }

    public ProductResponse getProductById(UUID id) {
        if (id == null)
            throw new IllegalArgumentException("Product id can not be null");
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toDTO(product);
    }
}
