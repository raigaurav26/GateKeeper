package com.gatekeeper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @RequestMapping(value = "/{service}/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<Map<String, Object>> handleRequest(
            @PathVariable String service,
            @RequestHeader("X-API-Key") String apiKey) {

        return ResponseEntity.ok(Map.of(
                "status", "forwarded",
                "service", service,
                "message", "Request processed by GateKeeper",
                "timestamp", LocalDateTime.now().toString(),
                "note", "In production, this would proxy to the actual backend service"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "GateKeeper API Gateway",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
