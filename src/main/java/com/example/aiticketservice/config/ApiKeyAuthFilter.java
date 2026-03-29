package com.example.aiticketservice.config;

import com.example.aiticketservice.client.config.AppProperties;
import com.example.aiticketservice.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String CALLER_ID_ATTR = "audit.callerId";
    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiKeyAuthFilter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        AppProperties.Security security = appProperties.getSecurity();
        String path = request.getRequestURI();

        request.setAttribute(CALLER_ID_ATTR, extractCallerId(request, security));

        if (!security.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (matchesAny(path, security.getPublicPaths()) || !matchesAny(path, security.getProtectedPaths())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isBlank(security.getApiKey())) {
            log.error("API key auth is enabled but app.security.api-key is not configured");
            writeJson(response, HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable");
            return;
        }

        String providedKey = request.getHeader(security.getHeaderName());
        if (!security.getApiKey().equals(providedKey)) {
            log.warn("Unauthorized request path={} method={} caller={} header={}",
                    path,
                    request.getMethod(),
                    request.getAttribute(CALLER_ID_ATTR),
                    security.getHeaderName());
            writeJson(response, HttpStatus.UNAUTHORIZED, "Unauthorized");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matchesAny(String path, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String extractCallerId(HttpServletRequest request, AppProperties.Security security) {
        String callerId = request.getHeader(security.getClientIdHeader());
        if (isBlank(callerId)) {
            callerId = request.getRemoteAddr();
        }
        return SensitiveLogSanitizer.sanitize(callerId);
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(message));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
