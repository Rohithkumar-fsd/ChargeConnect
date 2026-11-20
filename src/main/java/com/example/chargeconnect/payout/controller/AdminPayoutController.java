package com.example.chargeconnect.payout.controller;

import com.example.chargeconnect.payout.dto.PayoutResponse;
import com.example.chargeconnect.payout.dto.ProcessPayoutRequest;
import com.example.chargeconnect.payout.dto.RejectPayoutRequest;
import com.example.chargeconnect.payout.model.PayoutStatus;
import com.example.chargeconnect.payout.service.PayoutService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payouts")
public class AdminPayoutController {

    private final PayoutService payoutService;

    public AdminPayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @GetMapping
    public List<PayoutResponse> getAllPayouts(@RequestParam(required = false) PayoutStatus status) {
        return payoutService.getAllPayouts(status);
    }

    @PostMapping("/{id}/process")
    public PayoutResponse processPayout(
            @PathVariable Long id,
            @Valid @RequestBody ProcessPayoutRequest request
    ) {
        return payoutService.processPayout(id, request);
    }

    @PostMapping("/{id}/reject")
    public PayoutResponse rejectPayout(
            @PathVariable Long id,
            @Valid @RequestBody RejectPayoutRequest request
    ) {
        return payoutService.rejectPayout(id, request);
    }
}
