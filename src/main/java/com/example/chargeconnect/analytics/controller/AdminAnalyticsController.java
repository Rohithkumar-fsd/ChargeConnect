package com.example.chargeconnect.analytics.controller;

import com.example.chargeconnect.analytics.dto.AdminAnalyticsResponse;
import com.example.chargeconnect.analytics.service.AdminAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AdminAnalyticsResponse getAnalytics() {
        return analyticsService.getPlatformAnalytics();
    }
}
