package nova.enterprise_service_hub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to enforce strict OWASP security headers on every response.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Enforce HTTPS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // Prevent MIME-sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent Clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Enable Cross-Site Scripting (XSS) filter
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Control Referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Restrict powerful browser features
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        // Only allow scripts/styles from the same origin (adjust as needed for CDN)
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");

        filterChain.doFilter(request, response);
    }
}
