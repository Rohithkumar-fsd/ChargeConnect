package com.example.chargeconnect.review.dto;

import com.example.chargeconnect.review.model.Review;
import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long sessionId,
        Long bookingId,
        Long chargerId,
        String chargerTitle,
        Long driverId,
        String driverName,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getSession().getId(),
                review.getBooking().getId(),
                review.getCharger().getId(),
                review.getCharger().getTitle(),
                review.getDriver().getId(),
                review.getDriver().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
