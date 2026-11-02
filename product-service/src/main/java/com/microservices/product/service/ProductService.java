package com.microservices.product.service;

import com.microservices.product.dto.ProductRequest;
import com.microservices.product.dto.ProductResponse;
import com.microservices.product.dto.StockUpdateRequest;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    List<ProductResponse> getAllProducts(String category);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    ProductResponse deductStock(Long productId, StockUpdateRequest request);

    ProductResponse restoreStock(Long productId, StockUpdateRequest request);
}
