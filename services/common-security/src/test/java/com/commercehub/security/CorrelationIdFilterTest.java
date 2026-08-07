package com.commercehub.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesCorrelationIdWhenMissing() throws Exception {
        Environment env = new MockEnvironment().withProperty("spring.application.name", "auth-service");
        CorrelationIdFilter filter = new CorrelationIdFilter(env);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String header = response.getHeader(CorrelationIdConstants.HEADER);
        assertThat(header).isNotBlank();
        assertThat(MDC.get(CorrelationIdConstants.MDC_CORRELATION_ID)).isNull();
    }

    @Test
    void reusesIncomingCorrelationId() throws Exception {
        Environment env = new MockEnvironment().withProperty("spring.application.name", "auth-service");
        CorrelationIdFilter filter = new CorrelationIdFilter(env);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdConstants.HEADER, "cid-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(
                    jakarta.servlet.ServletRequest servletRequest,
                    jakarta.servlet.ServletResponse servletResponse
            ) {
                assertThat(MDC.get(CorrelationIdConstants.MDC_CORRELATION_ID)).isEqualTo("cid-123");
                assertThat(MDC.get(CorrelationIdConstants.MDC_SERVICE_NAME)).isEqualTo("auth-service");
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdConstants.HEADER)).isEqualTo("cid-123");
    }
}
