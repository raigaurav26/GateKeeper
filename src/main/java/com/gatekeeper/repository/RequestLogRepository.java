package com.gatekeeper.repository;

import com.gatekeeper.model.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    List<RequestLog> findByApiKeyValue(String apiKeyValue);

    long countByTimestampAfter(LocalDateTime time);

    long countByRateLimitedTrueAndTimestampAfter(LocalDateTime time);

    @Query("SELECT r.path, COUNT(r) as cnt FROM RequestLog r WHERE r.timestamp > :since GROUP BY r.path ORDER BY cnt DESC")
    List<Object[]> findTopEndpoints(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(r.responseTimeMs) FROM RequestLog r WHERE r.timestamp > :since")
    Double findAvgResponseTime(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM RequestLog r WHERE r.statusCode >= 200 AND r.statusCode < 400 AND r.timestamp > :since")
    long countSuccessRequests(@Param("since") LocalDateTime since);

    @Query("SELECT HOUR(r.timestamp) as hr, COUNT(r) as cnt FROM RequestLog r WHERE r.timestamp > :since GROUP BY HOUR(r.timestamp) ORDER BY hr")
    List<Object[]> findHourlyVolume(@Param("since") LocalDateTime since);

    List<RequestLog> findByApiKeyValueAndTimestampAfter(String apiKeyValue, LocalDateTime since);
}
