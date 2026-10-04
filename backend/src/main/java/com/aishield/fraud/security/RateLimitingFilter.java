package com.aishield.fraud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token-bucket / sliding window rate limiting filter for API security.
 * Defends against brute-force authentication attacks and transaction ingestion floods.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int AUTH_LIMIT_PER_MINUTE = 30;
    private static final int TRANSACTION_LIMIT_PER_MINUTE = 120;
    private static final long WINDOW_MILLIS = 60_000L; // 1 minute window

    private final Map<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class WindowCounter {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();

        int incrementAndGet(long now) {
            if (now - windowStart > WINDOW_MILLIS) {
                synchronized (this) {
                    if (now - windowStart > WINDOW_MILLIS) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
            return count.incrementAndGet();
        }

        int getRemaining(int limit) {
            return Math.max(0, limit - count.get());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate limit Auth endpoints and Transaction ingestion POST requests
        boolean isAuth = path.startsWith("/api/v1/auth");
        boolean isTxIngest = path.startsWith("/api/v1/transactions") && "POST".equalsIgnoreCase(method);

        if (!isAuth && !isTxIngest) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String bucketKey = (isAuth ? "auth:" : "tx:") + clientIp;
        int limit = isAuth ? AUTH_LIMIT_PER_MINUTE : TRANSACTION_LIMIT_PER_MINUTE;

        long now = System.currentTimeMillis();
        WindowCounter counter = requestCounts.computeIfAbsent(bucketKey, k -> new WindowCounter());
        int currentCount = counter.incrementAndGet(now);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(counter.getRemaining(limit)));

        if (currentCount > limit) {
            log.warn("Rate limit exceeded for IP {} on bucket {} (count: {}/{})", clientIp, bucketKey, currentCount, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");

            Map<String, Object> errorBody = Map.of(
                    "success", false,
                    "message", "Rate limit exceeded. Too many requests, please retry after 60 seconds."
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return;
        }

        // Clean up stale IP buckets periodically if map grows large
        if (requestCounts.size() > 5000) {
            requestCounts.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > (WINDOW_MILLIS * 2));
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
