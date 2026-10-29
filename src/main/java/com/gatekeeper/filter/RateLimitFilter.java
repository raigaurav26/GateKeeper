package com.gatekeeper.filter;

import com.gatekeeper.model.ApiKey;
import com.gatekeeper.service.ApiKeyService;
import com.gatekeeper.service.RateLimitDecision;
import com.gatekeeper.service.RateLimiterService;
import com.gatekeeper.service.RequestLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RequestLogService requestLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/gateway/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = request.getHeader("X-API-Key");
        long startTime = System.currentTimeMillis();

        if (apiKeyHeader == null || apiKeyHeader.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing X-API-Key header\"}");
            requestLogService.log("unknown", request.getMethod(), path, 401, 0,
                    request.getRemoteAddr(), false);
            return;
        }

        ApiKey apiKey = apiKeyService.validateKey(apiKeyHeader);
        if (apiKey == null) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or revoked API key\"}");
            requestLogService.log(apiKeyHeader, request.getMethod(), path, 403, 0,
                    request.getRemoteAddr(), false);
            return;
        }

        RateLimitDecision decision = rateLimiterService.tryConsume(apiKeyHeader, apiKey.getRateLimit());

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetSeconds()));

        if (!decision.allowed()) {
            long elapsed = System.currentTimeMillis() - startTime;
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Upgrade your plan or wait.\"}");
            requestLogService.log(apiKeyHeader, request.getMethod(), path, 429, elapsed,
                    request.getRemoteAddr(), true);
            return;
        }

        apiKeyService.recordUsage(apiKeyHeader);
        filterChain.doFilter(request, response);

        long elapsed = System.currentTimeMillis() - startTime;
        requestLogService.log(apiKeyHeader, request.getMethod(), path, response.getStatus(), elapsed,
                request.getRemoteAddr(), false);
    }
}
