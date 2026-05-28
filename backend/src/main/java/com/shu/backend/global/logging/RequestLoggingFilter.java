package com.shu.backend.global.logging;

import com.shu.backend.domain.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String requestId = resolveRequestId(request);

        MDC.put(REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);
        String userId = resolveUserId();
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("HTTP request completed: method={}, uri={}, status={}, elapsedMs={}, ip={}, userId={}",
                        request.getMethod(), request.getRequestURI(), status, elapsedMs, clientIp(request), userId);
            } else if (status >= 400) {
                log.warn("HTTP request completed: method={}, uri={}, status={}, elapsedMs={}, ip={}, userId={}",
                        request.getMethod(), request.getRequestURI(), status, elapsedMs, clientIp(request), userId);
            } else {
                log.info("HTTP request completed: method={}, uri={}, status={}, elapsedMs={}, ip={}, userId={}",
                        request.getMethod(), request.getRequestURI(), status, elapsedMs, clientIp(request), userId);
            }

            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/health")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/uploads/");
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return String.valueOf(user.getId());
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
