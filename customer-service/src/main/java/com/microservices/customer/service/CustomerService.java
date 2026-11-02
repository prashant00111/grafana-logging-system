package com.microservices.customer.service;

import com.microservices.customer.dto.CustomerRequest;
import com.microservices.customer.dto.CustomerResponse;
import com.microservices.customer.dto.WalletTransactionRequest;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse getCustomerByEmail(String email);

    List<CustomerResponse> getAllCustomers();

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    void deleteCustomer(Long id);

    CustomerResponse depositToWallet(Long customerId, WalletTransactionRequest request);

    CustomerResponse deductFromWallet(Long customerId, WalletTransactionRequest request);
}
