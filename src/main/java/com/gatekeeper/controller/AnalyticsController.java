package com.gatekeeper.controller;

import com.gatekeeper.dto.AnalyticsResponse;
import com.gatekeeper.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsResponse> getSummary(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getSummary(hours));
    }

    @GetMapping("/top-endpoints")
    public ResponseEntity<List<Map<String, Object>>> getTopEndpoints(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getTopEndpoints(hours));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<Map<String, Object>>> getHourlyVolume(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getHourlyVolume(hours));
    }

    @GetMapping("/key/{keyId}")
    public ResponseEntity<Map<String, Object>> getKeyAnalytics(
            @PathVariable Long keyId,
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getKeyAnalytics(keyId, hours));
    }
}
