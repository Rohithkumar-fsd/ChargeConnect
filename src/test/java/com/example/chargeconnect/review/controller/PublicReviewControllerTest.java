package com.example.chargeconnect.review.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.review.dto.ReviewResponse;
import com.example.chargeconnect.review.service.ReviewService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getChargerReviews_Returns200() throws Exception {
        ReviewResponse response = new ReviewResponse(
                1L, 100L, 10L, 2L, "Indiranagar Hub", 1L, "Rahul Driver",
                5, "Great experience", Instant.now(), Instant.now()
        );

        when(reviewService.getReviewsForCharger(2L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/chargers/2/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].driverName").value("Rahul Driver"));
    }
}
