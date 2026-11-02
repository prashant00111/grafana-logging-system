package com.microservices.customer.controller;

import com.microservices.customer.dto.ApiResponse;
import com.microservices.customer.dto.CustomerRequest;
import com.microservices.customer.dto.CustomerResponse;
import com.microservices.customer.dto.WalletTransactionRequest;
import com.microservices.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Endpoints for managing customers and wallet balances")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Register new customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        log.info("REST request to create customer: {}", request.getEmail());
        CustomerResponse response = customerService.createCustomer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Customer created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        log.info("REST request to get customer ID: {}", id);
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get customer by email")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByEmail(@PathVariable String email) {
        log.info("REST request to get customer by email: {}", email);
        CustomerResponse response = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List all customers")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        log.info("REST request to list all customers");
        List<CustomerResponse> responses = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer details")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(@PathVariable Long id,
                                                                        @Valid @RequestBody CustomerRequest request) {
        log.info("REST request to update customer ID: {}", id);
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (close) customer account")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        log.info("REST request to delete customer ID: {}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer account closed successfully"));
    }

    @PostMapping("/{id}/wallet/deposit")
    @Operation(summary = "Deposit funds into customer wallet")
    public ResponseEntity<ApiResponse<CustomerResponse>> depositToWallet(@PathVariable Long id,
                                                                         @Valid @RequestBody WalletTransactionRequest request) {
        log.info("REST request to deposit funds for customer ID: {}", id);
        CustomerResponse response = customerService.depositToWallet(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Funds deposited successfully"));
    }

    @PostMapping("/{id}/wallet/deduct")
    @Operation(summary = "Deduct funds from customer wallet")
    public ResponseEntity<ApiResponse<CustomerResponse>> deductFromWallet(@PathVariable Long id,
                                                                          @Valid @RequestBody WalletTransactionRequest request) {
        log.info("REST request to deduct funds for customer ID: {}", id);
        CustomerResponse response = customerService.deductFromWallet(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Funds deducted successfully"));
    }
}
