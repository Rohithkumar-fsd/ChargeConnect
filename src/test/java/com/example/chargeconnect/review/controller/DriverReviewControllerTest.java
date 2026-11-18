package com.example.chargeconnect.review.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.review.dto.CreateReviewRequest;
import com.example.chargeconnect.review.dto.ReviewResponse;
import com.example.chargeconnect.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication driverAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DriverReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        driverAuth = new UsernamePasswordAuthenticationToken("driver@test.com", null);
    }

    @Test
    void createReview_Returns201() throws Exception {
        ReviewResponse response = new ReviewResponse(
                1L, 100L, 10L, 2L, "Indiranagar Hub", 1L, "Rahul Driver",
                5, "Super convenient", Instant.now(), Instant.now()
        );

        when(reviewService.createReview(eq("driver@test.com"), any(CreateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/driver/reviews")
                        .principal(driverAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReviewRequest(100L, 5, "Super convenient"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Super convenient"));
    }

    @Test
    void createReview_InvalidRating_Returns400() throws Exception {
        mockMvc.perform(post("/api/driver/reviews")
                        .principal(driverAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReviewRequest(100L, 6, "Invalid 6 stars"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyReviews_Returns200() throws Exception {
        ReviewResponse response = new ReviewResponse(
                1L, 100L, 10L, 2L, "Indiranagar Hub", 1L, "Rahul Driver",
                5, "Super convenient", Instant.now(), Instant.now()
        );

        when(reviewService.getMyReviews("driver@test.com"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/driver/reviews")
                        .principal(driverAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }
}
