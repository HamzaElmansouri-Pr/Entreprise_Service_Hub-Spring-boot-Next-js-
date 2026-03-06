package nova.enterprise_service_hub.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter — validates rate limiting behavior.
 */
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("should allow requests within rate limit")
    void allowsRequestsWithinLimit() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    @DisplayName("should block requests exceeding rate limit")
    void blocksExcessiveRequests() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Exhaust the rate limit (200 requests/minute)
        for (int i = 0; i < 200; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }

        // 201st request should be blocked
        rateLimitFilter.doFilter(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        verify(response, atLeastOnce()).setContentType("application/json");
    }

    @Test
    @DisplayName("should track rate limits per IP")
    void tracksPerIp() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        rateLimitFilter.doFilter(request, response, filterChain);

        HttpServletRequest otherRequest = mock(HttpServletRequest.class);
        when(otherRequest.getRemoteAddr()).thenReturn("10.0.0.3");
        rateLimitFilter.doFilter(otherRequest, response, filterChain);

        // Both should pass — different IPs, separate buckets
        verify(filterChain, times(2)).doFilter(any(), any());
    }
}
