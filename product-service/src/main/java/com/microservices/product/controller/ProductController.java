package com.microservices.product.controller;

import com.microservices.product.dto.ApiResponse;
import com.microservices.product.dto.ProductRequest;
import com.microservices.product.dto.ProductResponse;
import com.microservices.product.dto.StockUpdateRequest;
import com.microservices.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Endpoints for managing products, inventory, and stock levels")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("REST request to create product SKU: {}", request.getSku());
        ProductResponse response = productService.createProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Product created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        log.info("REST request to get product ID: {}", id);
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        log.info("REST request to get product by SKU: {}", sku);
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List all products with optional category filter")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category) {
        log.info("REST request to list products. Category: {}", category);
        List<ProductResponse> responses = productService.getAllProducts(category);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product details")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Long id,
                                                                      @Valid @RequestBody ProductRequest request) {
        log.info("REST request to update product ID: {}", id);
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Discontinue product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("REST request to discontinue product ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product discontinued successfully"));
    }

    @PostMapping("/{id}/deduct-stock")
    @Operation(summary = "Deduct inventory stock for an order")
    public ResponseEntity<ApiResponse<ProductResponse>> deductStock(@PathVariable Long id,
                                                                    @Valid @RequestBody StockUpdateRequest request) {
        log.info("REST request to deduct stock for product ID: {}", id);
        ProductResponse response = productService.deductStock(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock deducted successfully"));
    }

    @PostMapping("/{id}/restore-stock")
    @Operation(summary = "Restore inventory stock for a cancelled order")
    public ResponseEntity<ApiResponse<ProductResponse>> restoreStock(@PathVariable Long id,
                                                                     @Valid @RequestBody StockUpdateRequest request) {
        log.info("REST request to restore stock for product ID: {}", id);
        ProductResponse response = productService.restoreStock(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock restored successfully"));
    }
}
