package com.microservices.payment.repository;

import com.microservices.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findByOrderNumber(String orderNumber);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
