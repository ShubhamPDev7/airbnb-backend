package com.codingshuttle.projects.airBnbApp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(20)
                .refillGreedy(20, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String ipAddress = request.getRemoteAddr();
        Bucket bucket = bucketCache.computeIfAbsent(ipAddress, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            log.info("Request allowed for IP: {}, remaining tokens: {}",
                    ipAddress, bucket.getAvailableTokens());
            return true;
        }

        log.warn("Rate limit exceeded for IP: {}", ipAddress);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        return false;
    }

}
