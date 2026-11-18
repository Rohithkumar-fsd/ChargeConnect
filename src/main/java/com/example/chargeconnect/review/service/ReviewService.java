package com.example.chargeconnect.review.service;

import com.example.chargeconnect.charger.model.Charger;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ChargingSessionRepository sessionRepository;
    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            ChargingSessionRepository sessionRepository,
            ChargerRepository chargerRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Submit a verified review for a completed charging session.
     */
    @Transactional
    public ReviewResponse createReview(String driverEmail, CreateReviewRequest request) {
        User driver = getUserByEmail(driverEmail);

        // 1. Session must exist and belong to this driver
        ChargingSession session = sessionRepository.findByIdAndDriverId(request.sessionId(), driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charging session not found for authenticated driver"));

        // 2. Only COMPLETED sessions can be reviewed
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only COMPLETED charging sessions can be reviewed. Current status: " + session.getStatus()
            );
        }

        // 3. One review per session
        if (reviewRepository.existsBySessionId(session.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already submitted a review for this charging session"
            );
        }

        // 4. Create and save review
        Charger charger = session.getCharger();
        Review review = new Review(
                session,
                session.getBooking(),
                charger,
                driver,
                request.rating(),
                request.comment()
        );
        Review saved = reviewRepository.save(review);

        // 5. Update charger average rating and total reviews
        Double avg = reviewRepository.calculateAverageRatingForCharger(charger.getId());
        int count = reviewRepository.countReviewsForCharger(charger.getId());

        charger.setAverageRating(BigDecimal.valueOf(avg != null ? avg : 0.0).setScale(2, RoundingMode.HALF_UP));
        charger.setTotalReviews(count);
        chargerRepository.save(charger);

        log.info("Review #{} submitted for charger '{}' by driver '{}' (Rating: {}/5, new average: {})",
                saved.getId(), charger.getTitle(), driver.getEmail(), request.rating(), charger.getAverageRating());

        return ReviewResponse.from(saved);
    }

    /**
     * Fetch all reviews for a specific charger.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForCharger(Long chargerId) {
        if (!chargerRepository.existsById(chargerId)) {
            throw new ResourceNotFoundException("Charger not found with id: " + chargerId);
        }
        return reviewRepository.findAllByChargerIdOrderByCreatedAtDesc(chargerId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * Fetch all reviews submitted by the authenticated driver.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(String driverEmail) {
        User driver = getUserByEmail(driverEmail);
        return reviewRepository.findAllByDriverIdOrderByCreatedAtDesc(driver.getId()).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * Fetch review for a specific session.
     */
    @Transactional(readOnly = true)
    public ReviewResponse getReviewForSession(String driverEmail, Long sessionId) {
        User driver = getUserByEmail(driverEmail);
        sessionRepository.findByIdAndDriverId(sessionId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charging session not found for driver"));

        Review review = reviewRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No review found for this charging session"));

        return ReviewResponse.from(review);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
