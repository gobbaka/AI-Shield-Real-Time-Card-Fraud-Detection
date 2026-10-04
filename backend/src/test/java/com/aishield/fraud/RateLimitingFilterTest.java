package com.aishield.fraud;

import com.aishield.fraud.security.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Requests within limit should pass through filterChain")
    void testRequestsWithinLimitPass() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader("X-RateLimit-Limit"));
        assertEquals("30", response.getHeader("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Requests exceeding limit on auth endpoint should be rejected with HTTP 429")
    void testAuthRateLimitExceeded() throws ServletException, IOException {
        String clientIp = "192.168.1.200";

        // Exceed limit of 30 requests
        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
        }

        // 31st request should be rejected with 429
        MockHttpServletRequest excessReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        excessReq.setRemoteAddr(clientIp);
        MockHttpServletResponse excessRes = new MockHttpServletResponse();

        filter.doFilter(excessReq, excessRes, filterChain);

        assertEquals(429, excessRes.getStatus());
        assertEquals("60", excessRes.getHeader("Retry-After"));
        assertTrue(excessRes.getContentAsString().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("Unrestricted endpoints should bypass rate limiting")
    void testUnrestrictedEndpointsBypass() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(response.getHeader("X-RateLimit-Limit"));
    }
}
