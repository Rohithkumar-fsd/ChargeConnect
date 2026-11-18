package com.example.chargeconnect.review.repo;

import com.example.chargeconnect.review.model.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsBySessionId(Long sessionId);

    Optional<Review> findBySessionId(Long sessionId);

    Optional<Review> findByIdAndDriverId(Long id, Long driverId);

    @EntityGraph(attributePaths = {"driver", "charger"})
    List<Review> findAllByChargerIdOrderByCreatedAtDesc(Long chargerId);

    @EntityGraph(attributePaths = {"charger"})
    List<Review> findAllByDriverIdOrderByCreatedAtDesc(Long driverId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.charger.id = :chargerId")
    Double calculateAverageRatingForCharger(@Param("chargerId") Long chargerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.charger.id = :chargerId")
    int countReviewsForCharger(@Param("chargerId") Long chargerId);
}
