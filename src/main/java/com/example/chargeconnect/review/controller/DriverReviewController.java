package com.example.chargeconnect.review.controller;

import com.example.chargeconnect.review.dto.CreateReviewRequest;
import com.example.chargeconnect.review.dto.ReviewResponse;
import com.example.chargeconnect.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/reviews")
public class DriverReviewController {

    private final ReviewService reviewService;

    public DriverReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Driver submits a verified review for a completed charging session.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return reviewService.createReview(authentication.getName(), request);
    }

    /**
     * List all reviews submitted by the authenticated driver.
     */
    @GetMapping
    public List<ReviewResponse> getMyReviews(Authentication authentication) {
        return reviewService.getMyReviews(authentication.getName());
    }

    /**
     * Get review for a specific charging session.
     */
    @GetMapping("/session/{sessionId}")
    public ReviewResponse getReviewForSession(
            Authentication authentication,
            @PathVariable Long sessionId
    ) {
        return reviewService.getReviewForSession(authentication.getName(), sessionId);
    }

    /**
     * List all reviews for a specific charger.
     */
    @GetMapping("/charger/{chargerId}")
    public List<ReviewResponse> getReviewsForCharger(@PathVariable Long chargerId) {
        return reviewService.getReviewsForCharger(chargerId);
    }
}
