package com.example.chargeconnect.payment.repo;

import com.example.chargeconnect.payment.model.Refund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findAllByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);
}