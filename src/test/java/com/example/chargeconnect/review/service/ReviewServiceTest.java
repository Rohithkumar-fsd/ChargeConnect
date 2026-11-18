package com.example.chargeconnect.review.service;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.model.LocationType;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.charger.repo.ChargerRepository;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.review.dto.CreateReviewRequest;
import com.example.chargeconnect.review.dto.ReviewResponse;
import com.example.chargeconnect.review.model.Review;
import com.example.chargeconnect.review.repo.ReviewRepository;
import com.example.chargeconnect.session.model.ChargingSession;
import com.example.chargeconnect.session.model.SessionStatus;
import com.example.chargeconnect.session.repo.ChargingSessionRepository;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.Vehicle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ChargingSessionRepository sessionRepository;
    @Mock private ChargerRepository chargerRepository;
    @Mock private UserRepository userRepository;

    private ReviewService reviewService;

    private User driver;
    private User host;
    private Charger charger;
    private Vehicle vehicle;
    private Booking booking;
    private ChargingSession session;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewRepository,
                sessionRepository,
                chargerRepository,
                userRepository
        );

        driver = new User("Rahul Driver", "driver@test.com", "+919876543210", "hash");
        setId(driver, 1L);

        host = new User("Kiran Host", "host@test.com", "+919876543211", "hash");
        setId(host, 2L);

        charger = new Charger(
                host, "Indiranagar Hub", "50kW Fast", LocationType.COMMERCIAL,
                "100 Feet Rd", "Bengaluru", "Karnataka", "560038", "India",
                new BigDecimal("12.971600"), new BigDecimal("77.594600"),
                PowerType.DC, new BigDecimal("50.00"), new BigDecimal("15.00")
        );
        setId(charger, 2L);

        vehicle = new Vehicle(
                driver, "Tata", "Nexon EV", "KA-01-EV-1234",
                new BigDecimal("40.50"), new BigDecimal("7.20"), new BigDecimal("30.00")
        );
        setId(vehicle, 3L);

        booking = new Booking(
                driver, charger, vehicle, Instant.now().minus(2, ChronoUnit.HOURS), Instant.now(),
                new BigDecimal("2.00"), new BigDecimal("15.00"), new BigDecimal("60.00"), new BigDecimal("900.00"),
                BookingStatus.COMPLETED
        );
        setId(booking, 10L);

        session = new ChargingSession(booking, driver, charger, vehicle, BigDecimal.ZERO);
        session.complete(Instant.now(), "Completed normally");
        setId(session, 100L);
    }

    @Test
    void createReview_Success_SavesReviewAndUpdatesChargerAverageRating() {
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(sessionRepository.findByIdAndDriverId(100L, driver.getId())).thenReturn(Optional.of(session));
        when(reviewRepository.existsBySessionId(100L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            setId(r, 1L);
            return r;
        });
        when(reviewRepository.calculateAverageRatingForCharger(2L)).thenReturn(5.0);
        when(reviewRepository.countReviewsForCharger(2L)).thenReturn(1);
        when(chargerRepository.save(charger)).thenReturn(charger);

        ReviewResponse response = reviewService.createReview(
                driver.getEmail(), new CreateReviewRequest(100L, 5, "Amazing fast charging!")
        );

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Amazing fast charging!");
        assertThat(charger.getAverageRating()).isEqualByComparingTo("5.00");
        assertThat(charger.getTotalReviews()).isEqualTo(1);
    }

    @Test
    void createReview_SessionNotCompleted_Throws400() {
        ChargingSession activeSession = new ChargingSession(booking, driver, charger, vehicle, BigDecimal.ZERO);
        setId(activeSession, 101L);

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(sessionRepository.findByIdAndDriverId(101L, driver.getId())).thenReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> reviewService.createReview(
                driver.getEmail(), new CreateReviewRequest(101L, 4, "Too early")
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only COMPLETED charging sessions can be reviewed");
    }

    @Test
    void createReview_DuplicateReviewForSession_Throws409() {
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(sessionRepository.findByIdAndDriverId(100L, driver.getId())).thenReturn(Optional.of(session));
        when(reviewRepository.existsBySessionId(100L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(
                driver.getEmail(), new CreateReviewRequest(100L, 5, "Reviewing again")
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already submitted a review");
    }

    @Test
    void getReviewsForCharger_ReturnsReviews() {
        Review review = new Review(session, booking, charger, driver, 5, "Great host");
        setId(review, 1L);

        when(chargerRepository.existsById(2L)).thenReturn(true);
        when(reviewRepository.findAllByChargerIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(review));

        List<ReviewResponse> reviews = reviewService.getReviewsForCharger(2L);

        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).rating()).isEqualTo(5);
    }

    @Test
    void getMyReviews_ReturnsDriverReviews() {
        Review review = new Review(session, booking, charger, driver, 5, "Great host");
        setId(review, 1L);

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(reviewRepository.findAllByDriverIdOrderByCreatedAtDesc(driver.getId())).thenReturn(List.of(review));

        List<ReviewResponse> reviews = reviewService.getMyReviews(driver.getEmail());

        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).comment()).isEqualTo("Great host");
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Reflection error", e);
        }
    }
}
