package com.example.chargeconnect.payout.controller;

import com.example.chargeconnect.payout.dto.HostEarningsSummaryResponse;
import com.example.chargeconnect.payout.dto.PayoutResponse;
import com.example.chargeconnect.payout.dto.RequestPayoutRequest;
import com.example.chargeconnect.payout.service.PayoutService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/payouts")
public class HostPayoutController {

    private final PayoutService payoutService;

    public HostPayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @GetMapping("/earnings")
    public HostEarningsSummaryResponse getEarningsSummary(Authentication authentication) {
        return payoutService.getHostEarningsSummary(authentication.getName());
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public PayoutResponse requestPayout(
            Authentication authentication,
            @Valid @RequestBody RequestPayoutRequest request
    ) {
        return payoutService.requestPayout(authentication.getName(), request);
    }

    @GetMapping
    public List<PayoutResponse> getPayouts(Authentication authentication) {
        return payoutService.getHostPayouts(authentication.getName());
    }
}
