package com.gatekeeper.service;

import com.gatekeeper.dto.AnalyticsResponse;
import com.gatekeeper.repository.RequestLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private RequestLogRepository requestLogRepository;

    public AnalyticsResponse getSummary(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        AnalyticsResponse response = new AnalyticsResponse();
        response.setTotalRequests(requestLogRepository.countByTimestampAfter(since));
        response.setBlockedRequests(requestLogRepository.countByRateLimitedTrueAndTimestampAfter(since));

        Double avgTime = requestLogRepository.findAvgResponseTime(since);
        response.setAvgResponseTimeMs(avgTime != null ? avgTime : 0);

        long success = requestLogRepository.countSuccessRequests(since);
        long total = response.getTotalRequests();
        response.setSuccessRate(total > 0 ? (double) success / total * 100 : 0);

        return response;
    }

    public List<Map<String, Object>> getTopEndpoints(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Object[]> results = requestLogRepository.findTopEndpoints(since);

        return results.stream()
                .limit(10)
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("endpoint", row[0]);
                    map.put("count", row[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getHourlyVolume(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Object[]> results = requestLogRepository.findHourlyVolume(since);

        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("hour", row[0]);
                    map.put("requests", row[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getKeyAnalytics(Long keyId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        var logs = requestLogRepository.findByApiKeyValueAndTimestampAfter(
                keyId.toString(), since);

        Map<String, Object> result = new HashMap<>();
        result.put("totalRequests", logs.size());
        result.put("rateLimited", logs.stream().filter(l -> l.isRateLimited()).count());
        result.put("avgResponseTime", logs.stream()
                .mapToLong(l -> l.getResponseTimeMs())
                .average().orElse(0));
        return result;
    }
}
