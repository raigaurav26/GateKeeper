package com.gatekeeper.service;

import com.gatekeeper.dto.ApiKeyRequest;
import com.gatekeeper.dto.ApiKeyResponse;
import com.gatekeeper.model.ApiKey;
import com.gatekeeper.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public ApiKeyResponse generateKey(ApiKeyRequest request) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyValue("gk_" + UUID.randomUUID().toString().replace("-", ""));
        apiKey.setName(request.getName());
        apiKey.setOwner(request.getOwner());

        String tier = request.getTier() != null ? request.getTier().toUpperCase() : "FREE";
        apiKey.setTier(tier);

        switch (tier) {
            case "PRO":
                apiKey.setRateLimit(1000);
                break;
            case "ENTERPRISE":
                apiKey.setRateLimit(10000);
                break;
            default:
                apiKey.setTier("FREE");
                apiKey.setRateLimit(100);
                break;
        }

        apiKey = apiKeyRepository.save(apiKey);
        return mapToResponse(apiKey);
    }

    public List<ApiKeyResponse> getAllKeys() {
        return apiKeyRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ApiKeyResponse getKeyById(Long id) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API key not found"));
        return mapToResponse(key);
    }

    public ApiKeyResponse updateKey(Long id, ApiKeyRequest request) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        if (request.getName() != null) key.setName(request.getName());
        if (request.getOwner() != null) key.setOwner(request.getOwner());
        if (request.getTier() != null) {
            String tier = request.getTier().toUpperCase();
            key.setTier(tier);
            switch (tier) {
                case "PRO": key.setRateLimit(1000); break;
                case "ENTERPRISE": key.setRateLimit(10000); break;
                default: key.setRateLimit(100); break;
            }
        }

        key = apiKeyRepository.save(key);
        return mapToResponse(key);
    }

    public void revokeKey(Long id) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API key not found"));
        key.setActive(false);
        apiKeyRepository.save(key);
    }

    public ApiKey validateKey(String keyValue) {
        return apiKeyRepository.findByKeyValueAndActiveTrue(keyValue)
                .orElse(null);
    }

    public void recordUsage(String keyValue) {
        apiKeyRepository.findByKeyValue(keyValue).ifPresent(key -> {
            key.setLastUsedAt(LocalDateTime.now());
            key.setTotalRequests(key.getTotalRequests() + 1);
            apiKeyRepository.save(key);
        });
    }

    private ApiKeyResponse mapToResponse(ApiKey key) {
        return new ApiKeyResponse(
                key.getId(), key.getKeyValue(), key.getName(), key.getOwner(),
                key.getTier(), key.getRateLimit(), key.isActive(),
                key.getCreatedAt(), key.getLastUsedAt(), key.getTotalRequests());
    }
}
