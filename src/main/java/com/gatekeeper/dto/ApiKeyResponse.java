package com.gatekeeper.dto;

import java.time.LocalDateTime;

public class ApiKeyResponse {

    private Long id;
    private String keyValue;
    private String name;
    private String owner;
    private String tier;
    private int rateLimit;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private long totalRequests;

    public ApiKeyResponse() {}

    public ApiKeyResponse(Long id, String keyValue, String name, String owner, String tier,
                          int rateLimit, boolean active, LocalDateTime createdAt,
                          LocalDateTime lastUsedAt, long totalRequests) {
        this.id = id;
        this.keyValue = keyValue;
        this.name = name;
        this.owner = owner;
        this.tier = tier;
        this.rateLimit = rateLimit;
        this.active = active;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.totalRequests = totalRequests;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public int getRateLimit() { return rateLimit; }
    public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
}
