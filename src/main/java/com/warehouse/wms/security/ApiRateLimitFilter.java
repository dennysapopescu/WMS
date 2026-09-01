package com.warehouse.wms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight per-IP fixed-window protection. Use a distributed limiter in a multi-instance deployment. */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;

    public ApiRateLimitFilter(@Value("${app.rate-limit.requests-per-minute:120}") int maxRequests) { this.maxRequests = maxRequests; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/api/"); }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String key = request.getRemoteAddr();
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(key, (ignored, current) -> current == null || current.minute != minute ? new Window(minute, 1) : new Window(minute, current.count + 1));
        if (window.count > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Window(long minute, int count) { }
}
