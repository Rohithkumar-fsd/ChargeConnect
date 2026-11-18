package com.example.chargeconnect.review.controller;

import com.example.chargeconnect.review.dto.ReviewResponse;
import com.example.chargeconnect.review.service.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chargers/{chargerId}/reviews")
public class PublicReviewController {

    private final ReviewService reviewService;

    public PublicReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Public endpoint: anyone can view reviews for an approved charger.
     */
    @GetMapping
    public List<ReviewResponse> getChargerReviews(@PathVariable Long chargerId) {
        return reviewService.getReviewsForCharger(chargerId);
    }
}
