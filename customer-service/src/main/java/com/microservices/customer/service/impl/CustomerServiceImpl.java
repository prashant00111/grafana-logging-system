package com.microservices.customer.service.impl;

import com.microservices.customer.dto.CustomerRequest;
import com.microservices.customer.dto.CustomerResponse;
import com.microservices.customer.dto.WalletTransactionRequest;
import com.microservices.customer.entity.Customer;
import com.microservices.customer.exception.CustomerAlreadyExistsException;
import com.microservices.customer.exception.CustomerNotFoundException;
import com.microservices.customer.exception.InsufficientBalanceException;
import com.microservices.customer.repository.CustomerRepository;
import com.microservices.customer.service.CustomerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final Counter customerCreatedCounter;
    private final Counter walletDeductCounter;

    public CustomerServiceImpl(CustomerRepository customerRepository, MeterRegistry meterRegistry) {
        this.customerRepository = customerRepository;
        this.customerCreatedCounter = Counter.builder("customers.created.count")
                .description("Number of customers registered")
                .register(meterRegistry);
        this.walletDeductCounter = Counter.builder("customers.wallet.deduct.count")
                .description("Number of wallet deductions")
                .register(meterRegistry);
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(request.getEmail());
        }

        BigDecimal initialBalance = (request.getInitialBalance() != null)
                ? request.getInitialBalance()
                : BigDecimal.ZERO;

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .walletBalance(initialBalance)
                .status(Customer.CustomerStatus.ACTIVE)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        customerCreatedCounter.increment();
        log.info("Successfully created customer id: {}, email: {}", savedCustomer.getId(), savedCustomer.getEmail());
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        log.info("Fetching customer with id: {}", id);
        return customerRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        log.info("Fetching customer with email: {}", email);
        return customerRepository.findByEmail(email)
                .map(this::mapToResponse)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with email '" + email + "' not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers");
        return customerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        log.info("Updating customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        if (!customer.getEmail().equalsIgnoreCase(request.getEmail()) && customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(request.getEmail());
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setAddress(request.getAddress());

        Customer updated = customerRepository.save(customer);
        log.info("Successfully updated customer id: {}", id);
        return mapToResponse(updated);
    }

    @Override
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        customer.setStatus(Customer.CustomerStatus.CLOSED);
        customerRepository.save(customer);
        log.info("Customer id: {} marked as CLOSED", id);
    }

    @Override
    public CustomerResponse depositToWallet(Long customerId, WalletTransactionRequest request) {
        log.info("Depositing {} to customer id: {}", request.getAmount(), customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customer.setWalletBalance(customer.getWalletBalance().add(request.getAmount()));
        Customer updated = customerRepository.save(customer);
        log.info("Deposit successful for customer id: {}. New balance: {}", customerId, updated.getWalletBalance());
        return mapToResponse(updated);
    }

    @Override
    public CustomerResponse deductFromWallet(Long customerId, WalletTransactionRequest request) {
        log.info("Deducting {} from customer id: {}", request.getAmount(), customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (customer.getWalletBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(customer.getWalletBalance(), request.getAmount());
        }

        customer.setWalletBalance(customer.getWalletBalance().subtract(request.getAmount()));
        Customer updated = customerRepository.save(customer);
        walletDeductCounter.increment();
        log.info("Deduction successful for customer id: {}. Remaining balance: {}", customerId, updated.getWalletBalance());
        return mapToResponse(updated);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .address(customer.getAddress())
                .walletBalance(customer.getWalletBalance())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
