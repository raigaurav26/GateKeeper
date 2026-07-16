package com.gatekeeper.controller;

import com.gatekeeper.dto.ApiKeyRequest;
import com.gatekeeper.dto.ApiKeyResponse;
import com.gatekeeper.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKeyResponse> generateKey(@RequestBody ApiKeyRequest request) {
        return new ResponseEntity<>(apiKeyService.generateKey(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getAllKeys() {
        return ResponseEntity.ok(apiKeyService.getAllKeys());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> getKeyById(@PathVariable Long id) {
        return ResponseEntity.ok(apiKeyService.getKeyById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> updateKey(@PathVariable Long id, @RequestBody ApiKeyRequest request) {
        return ResponseEntity.ok(apiKeyService.updateKey(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> revokeKey(@PathVariable Long id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.ok(Map.of("message", "API key revoked successfully"));
    }
}
