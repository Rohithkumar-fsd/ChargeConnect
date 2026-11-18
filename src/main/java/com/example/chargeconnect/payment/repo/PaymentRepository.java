package com.example.chargeconnect.payment.repo;

import com.example.chargeconnect.payment.model.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByBookingIdAndDriverId(Long bookingId, Long driverId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByIdAndDriverId(Long id, Long driverId);

    List<Payment> findAllByDriverIdOrderByCreatedAtDesc(Long driverId);
}