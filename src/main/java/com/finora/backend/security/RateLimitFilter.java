package com.finora.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();

        Bucket bucket;
        if (path.equals("/api/auth/login")) {
            // Strict: 5 login attempts per minute per IP — brute-force protection
            bucket = loginBuckets.computeIfAbsent(clientIp, ip ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                            .build());
        } else if (path.startsWith("/api/")) {
            // General: 60 requests per minute per IP — spam/abuse protection
            bucket = generalBuckets.computeIfAbsent(clientIp, ip ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                            .build());
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}