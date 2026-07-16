package com.gatekeeper.service;

import com.gatekeeper.model.RequestLog;
import com.gatekeeper.repository.RequestLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestLogService {

    @Autowired
    private RequestLogRepository requestLogRepository;

    public void log(String apiKeyValue, String method, String path, int statusCode,
                    long responseTimeMs, String clientIp, boolean rateLimited) {
        RequestLog log = new RequestLog();
        log.setApiKeyValue(apiKeyValue);
        log.setMethod(method);
        log.setPath(path);
        log.setStatusCode(statusCode);
        log.setResponseTimeMs(responseTimeMs);
        log.setClientIp(clientIp);
        log.setRateLimited(rateLimited);
        requestLogRepository.save(log);
    }
}
