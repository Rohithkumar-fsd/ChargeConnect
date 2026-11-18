package com.example.chargeconnect.payment.service;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.booking.repo.BookingRepository;
import com.example.chargeconnect.booking.service.BookingLockService;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.model.LocationType;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.payment.dto.CreatePaymentOrderRequest;
import com.example.chargeconnect.payment.dto.GatewayOrder;
import com.example.chargeconnect.payment.dto.GatewayRefund;
import com.example.chargeconnect.payment.dto.VerifyPaymentRequest;
import com.example.chargeconnect.payment.model.Payment;
import com.example.chargeconnect.payment.model.PaymentStatus;
import com.example.chargeconnect.payment.model.Refund;
import com.example.chargeconnect.payment.repo.PaymentRepository;
import com.example.chargeconnect.payment.repo.RefundRepository;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.Vehicle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private BookingLockService lockService;

    private PaymentService paymentService;

    private User driver;
    private User host;
    private Charger charger;
    private Vehicle vehicle;
    private Booking booking;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                refundRepository,
                bookingRepository,
                userRepository,
                paymentGateway,
                lockService
        );

        driver = new User("Rahul Driver", "driver@test.com", "+919876543210", "hash");
        host = new User("Kiran Host", "host@test.com", "+919876543211", "hash");

        charger = new Charger(
                host, "Station A", "Desc", LocationType.COMMERCIAL,
                "Address", "City", "State", "560001", "India",
                new BigDecimal("12.971600"), new BigDecimal("77.594600"),
                PowerType.DC, new BigDecimal("50.00"), new BigDecimal("15.00")
        );

        vehicle = new Vehicle(
                driver, "Tata", "Nexon EV", "KA-01-EV-1234",
                new BigDecimal("40.50"), new BigDecimal("7.20"), new BigDecimal("30.00")
        );

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        booking = new Booking(
                driver, charger, vehicle, start, end,
                new BigDecimal("2.00"), new BigDecimal("15.00"), new BigDecimal("60.00"), new BigDecimal("900.00"),
                BookingStatus.PENDING
        );
    }

    @Test
    void createPaymentOrderSuccessfullyCreatesGatewayOrderAndSavesPayment() {
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        GatewayOrder gatewayOrder = new GatewayOrder("order_12345", new BigDecimal("900.00"), "INR", "rzp_test_key");
        when(paymentGateway.createOrder(eq(new BigDecimal("900.00")), eq("INR"), any())).thenReturn(gatewayOrder);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return p;
        });

        var response = paymentService.createPaymentOrder(driver.getEmail(), new CreatePaymentOrderRequest(1L));

        assertThat(response.razorpayOrderId()).isEqualTo("order_12345");
        assertThat(response.amount()).isEqualByComparingTo("900.00");
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.keyId()).isEqualTo("rzp_test_key");

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPaymentOrderFailsWhenBookingAlreadyConfirmed() {
        booking.confirm();
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                paymentService.createPaymentOrder(driver.getEmail(), new CreatePaymentOrderRequest(1L))
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already paid and confirmed");

        verify(paymentGateway, never()).createOrder(any(), any(), any());
    }

    @Test
    void verifyPaymentConfirmsBookingAndReleasesRedisLock() {
        Payment payment = new Payment(booking, driver, "order_12345", new BigDecimal("900.00"), "INR");

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(payment));
        when(paymentGateway.verifySignature("order_12345", "pay_99999", "valid_sig")).thenReturn(true);
        when(paymentRepository.save(payment)).thenReturn(payment);

        VerifyPaymentRequest req = new VerifyPaymentRequest(1L, "order_12345", "pay_99999", "valid_sig");
        var response = paymentService.verifyPayment(driver.getEmail(), req);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(lockService).releaseLock(any(), eq(driver.getEmail()));
    }

    @Test
    void verifyPaymentFailsWhenSignatureIsInvalid() {
        Payment payment = new Payment(booking, driver, "order_12345", new BigDecimal("900.00"), "INR");

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(payment));
        when(paymentGateway.verifySignature("order_12345", "pay_99999", "fake_sig")).thenReturn(false);

        VerifyPaymentRequest req = new VerifyPaymentRequest(1L, "order_12345", "pay_99999", "fake_sig");

        assertThatThrownBy(() ->
                paymentService.verifyPayment(driver.getEmail(), req)
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid signature");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING); // Not confirmed!
        verify(lockService, never()).releaseLock(any(), any());
    }

    @Test
    void processRefundSuccessfullyRefundsPaymentAndCancelsBooking() {
        Payment payment = new Payment(booking, driver, "order_12345", new BigDecimal("900.00"), "INR");
        payment.markSuccess("pay_99999", "sig");
        booking.confirm();

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(payment));

        GatewayRefund gatewayRefund = new GatewayRefund("rfnd_001", new BigDecimal("900.00"), "PROCESSED");
        when(paymentGateway.refundPayment("pay_99999", new BigDecimal("900.00"), "Driver cancelled"))
                .thenReturn(gatewayRefund);

        when(paymentRepository.save(payment)).thenReturn(payment);

        var response = paymentService.processRefund(driver.getEmail(), 1L, "Driver cancelled");

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(refundRepository).save(any(Refund.class));
    }
}
