package com.example.chargeconnect.payment.service;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.booking.repo.BookingRepository;
import com.example.chargeconnect.booking.service.BookingLockService;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.payment.dto.*;
import com.example.chargeconnect.payment.model.Payment;
import com.example.chargeconnect.payment.model.PaymentStatus;
import com.example.chargeconnect.payment.model.Refund;
import com.example.chargeconnect.payment.repo.PaymentRepository;
import com.example.chargeconnect.payment.repo.RefundRepository;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final BookingLockService lockService;

    public PaymentService(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            PaymentGateway paymentGateway,
            BookingLockService lockService
    ) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.lockService = lockService;
    }

    @Transactional
    public PaymentOrderResponse createPaymentOrder(String driverEmail, CreatePaymentOrderRequest request) {
        User driver = getUserByEmail(driverEmail);
        Booking booking = bookingRepository.findByIdAndDriverId(request.bookingId(), driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for authenticated driver"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is already paid and confirmed");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is not eligible for payment (status: " + booking.getStatus() + ")");
        }

        // Idempotency: if a pending order already exists for this booking, return it
        Optional<Payment> existingPaymentOpt = paymentRepository.findByBookingId(booking.getId());
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            if (existing.getStatus() == PaymentStatus.PENDING) {
                return new PaymentOrderResponse(
                        existing.getId(),
                        booking.getId(),
                        existing.getRazorpayOrderId(),
                        existing.getAmount(),
                        existing.getCurrency(),
                        paymentGateway.getKeyId()
                );
            }
            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking has already been paid successfully");
            }
        }

        // Create order with gateway for the exact booking total price
        GatewayOrder gatewayOrder = paymentGateway.createOrder(
                booking.getTotalPrice(), "INR", booking.getBookingReference()
        );

        Payment payment = new Payment(
                booking,
                driver,
                gatewayOrder.orderId(),
                gatewayOrder.amount(),
                gatewayOrder.currency()
        );

        Payment saved = paymentRepository.save(payment);

        return new PaymentOrderResponse(
                saved.getId(),
                booking.getId(),
                saved.getRazorpayOrderId(),
                saved.getAmount(),
                saved.getCurrency(),
                gatewayOrder.keyId()
        );
    }

    @Transactional
    public PaymentResponse verifyPayment(String driverEmail, VerifyPaymentRequest request) {
        User driver = getUserByEmail(driverEmail);
        Booking booking = bookingRepository.findByIdAndDriverId(request.bookingId(), driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for authenticated driver"));

        Payment payment = paymentRepository.findByBookingIdAndDriverId(request.bookingId(), driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for this booking"));

        // If already verified, return idempotent response
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResponse.from(payment);
        }

        if (!payment.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID does not match the payment record");
        }

        // Cryptographic HMAC-SHA256 signature check
        boolean isValid = paymentGateway.verifySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature()
        );

        if (!isValid) {
            payment.markFailed("Invalid Razorpay cryptographic signature");
            paymentRepository.save(payment);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed: invalid signature");
        }

        // Transition states: Payment -> SUCCESS, Booking -> CONFIRMED
        payment.markSuccess(request.razorpayPaymentId(), request.razorpaySignature());
        booking.confirm();
        bookingRepository.save(booking);

        // Release the temporary Redis checkout hold lock
        String lockKey = "lock:charger:" + booking.getCharger().getId() + ":slot:"
                + booking.getStartTime().toEpochMilli() + ":" + booking.getEndTime().toEpochMilli();
        lockService.releaseLock(lockKey, driver.getEmail());

        log.info("Payment '{}' verified successfully for booking '{}'", payment.getId(), booking.getId());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(String driverEmail, Long bookingId) {
        User driver = getUserByEmail(driverEmail);
        Payment payment = paymentRepository.findByBookingIdAndDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this booking"));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getDriverPayments(String driverEmail) {
        User driver = getUserByEmail(driverEmail);
        return paymentRepository.findAllByDriverIdOrderByCreatedAtDesc(driver.getId())
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse processRefund(String driverEmail, Long bookingId, String reason) {
        User driver = getUserByEmail(driverEmail);
        Booking booking = bookingRepository.findByIdAndDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for authenticated driver"));

        Payment payment = paymentRepository.findByBookingIdAndDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this booking"));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot refund an unconfirmed or failed payment");
        }
        if (Instant.now().isAfter(booking.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot refund a charging session that has already started");
        }

        GatewayRefund gatewayRefund = paymentGateway.refundPayment(
                payment.getRazorpayPaymentId(), payment.getAmount(), reason
        );

        Refund refund = new Refund(
                payment, gatewayRefund.refundId(), gatewayRefund.amount(), reason
        );
        refundRepository.save(refund);

        payment.markRefunded();
        booking.cancel(reason != null ? reason : "Refunded upon cancellation");

        bookingRepository.save(booking);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}