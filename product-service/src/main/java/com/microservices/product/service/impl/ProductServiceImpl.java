package com.microservices.product.service.impl;

import com.microservices.product.dto.ProductRequest;
import com.microservices.product.dto.ProductResponse;
import com.microservices.product.dto.StockUpdateRequest;
import com.microservices.product.entity.Product;
import com.microservices.product.exception.DuplicateSkuException;
import com.microservices.product.exception.InsufficientStockException;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.repository.ProductRepository;
import com.microservices.product.service.ProductService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final Counter productCreatedCounter;
    private final Counter stockDeductCounter;

    public ProductServiceImpl(ProductRepository productRepository, MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.productCreatedCounter = Counter.builder("products.created.count")
                .description("Number of products created")
                .register(meterRegistry);
        this.stockDeductCounter = Counter.builder("products.stock.deducted.count")
                .description("Number of stock deductions")
                .register(meterRegistry);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        Product product = Product.builder()
                .sku(request.getSku().toUpperCase().trim())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .status(request.getStockQuantity() > 0 ? Product.ProductStatus.AVAILABLE : Product.ProductStatus.OUT_OF_STOCK)
                .build();

        Product saved = productRepository.save(product);
        productCreatedCounter.increment();
        log.info("Successfully created product id: {}, SKU: {}", saved.getId(), saved.getSku());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.info("Fetching product with SKU: {}", sku);
        return productRepository.findBySku(sku.toUpperCase().trim())
                .map(this::mapToResponse)
                .orElseThrow(() -> new ProductNotFoundException("Product with SKU '" + sku + "' not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String category) {
        log.info("Fetching products. Category filter: {}", category);
        List<Product> products = (category != null && !category.trim().isEmpty())
                ? productRepository.findByCategoryIgnoreCase(category.trim())
                : productRepository.findAll();

        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSku().equalsIgnoreCase(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        product.setSku(request.getSku().toUpperCase().trim());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setStatus(request.getStockQuantity() > 0 ? Product.ProductStatus.AVAILABLE : Product.ProductStatus.OUT_OF_STOCK);

        Product updated = productRepository.save(product);
        log.info("Successfully updated product id: {}", id);
        return mapToResponse(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Discontinuing product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStatus(Product.ProductStatus.DISCONTINUED);
        productRepository.save(product);
        log.info("Product id: {} marked as DISCONTINUED", id);
    }

    @Override
    public ProductResponse deductStock(Long productId, StockUpdateRequest request) {
        log.info("Deducting {} units from product id: {}, reason: {}", request.getQuantity(), productId, request.getReason());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getSku(), product.getStockQuantity(), request.getQuantity());
        }

        int remainingStock = product.getStockQuantity() - request.getQuantity();
        product.setStockQuantity(remainingStock);
        if (remainingStock == 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        }

        Product updated = productRepository.save(product);
        stockDeductCounter.increment();
        log.info("Stock deducted for product id: {}. Remaining stock: {}", productId, updated.getStockQuantity());
        return mapToResponse(updated);
    }

    @Override
    public ProductResponse restoreStock(Long productId, StockUpdateRequest request) {
        log.info("Restoring {} units to product id: {}, reason: {}", request.getQuantity(), productId, request.getReason());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        int newStock = product.getStockQuantity() + request.getQuantity();
        product.setStockQuantity(newStock);
        if (product.getStatus() == Product.ProductStatus.OUT_OF_STOCK && newStock > 0) {
            product.setStatus(Product.ProductStatus.AVAILABLE);
        }

        Product updated = productRepository.save(product);
        log.info("Stock restored for product id: {}. New stock: {}", productId, updated.getStockQuantity());
        return mapToResponse(updated);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
