package com.commercehub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final String serviceName;

    public CorrelationIdFilter(Environment environment) {
        this.serviceName = environment.getProperty("spring.application.name", "unknown");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = request.getHeader(CorrelationIdConstants.HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CorrelationIdConstants.MDC_CORRELATION_ID, correlationId);
        MDC.put(CorrelationIdConstants.MDC_SERVICE_NAME, serviceName);
        response.setHeader(CorrelationIdConstants.HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIdConstants.MDC_CORRELATION_ID);
            MDC.remove(CorrelationIdConstants.MDC_SERVICE_NAME);
        }
    }
}
