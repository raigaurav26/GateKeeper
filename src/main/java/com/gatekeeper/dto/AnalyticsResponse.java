package com.gatekeeper.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsResponse {

    private long totalRequests;
    private long blockedRequests;
    private double avgResponseTimeMs;
    private double successRate;
    private List<Map<String, Object>> topEndpoints;
    private List<Map<String, Object>> requestVolume;

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

    public long getBlockedRequests() { return blockedRequests; }
    public void setBlockedRequests(long blockedRequests) { this.blockedRequests = blockedRequests; }

    public double getAvgResponseTimeMs() { return avgResponseTimeMs; }
    public void setAvgResponseTimeMs(double avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public List<Map<String, Object>> getTopEndpoints() { return topEndpoints; }
    public void setTopEndpoints(List<Map<String, Object>> topEndpoints) { this.topEndpoints = topEndpoints; }

    public List<Map<String, Object>> getRequestVolume() { return requestVolume; }
    public void setRequestVolume(List<Map<String, Object>> requestVolume) { this.requestVolume = requestVolume; }
}
