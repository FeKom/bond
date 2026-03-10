package github.fekom.bond.infrastructure.web;

import github.fekom.bond.api.PayloadCompressor;
import github.fekom.bond.api.RateLimiterService;
import github.fekom.bond.domain.RequestResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that automatically applies rate limiting to every HTTP request.
 * <p>
 * Resolves client IP from {@code X-Forwarded-For}, {@code X-Real-IP}, or {@code remoteAddr}.
 * Calculates request size using GZIP-compressed body length.
 * Returns HTTP 429 with {@code Retry-After} header when the rate limit is exceeded.
 * <p>
 * Auto-configured when {@code spring-boot-starter-web} is on the classpath.
 * Disable with {@code bond.filter-enabled=false}.
 */
public class BondRateLimitFilter extends OncePerRequestFilter {

	private final RateLimiterService rateLimiterService;

	public BondRateLimitFilter(RateLimiterService rateLimiterService) {
		this.rateLimiterService = rateLimiterService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String ipAddress = resolveIp(request);
		String endpoint = request.getRequestURI();

		long requestSizeBytes = calculateSize(request);

		RequestResult result = rateLimiterService.checkRateLimit(ipAddress, endpoint, requestSizeBytes);

		if (result.blocked()) {
			response.setStatus(403);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"ip_blocked\"}");
			return;
		}

		response.setHeader("X-RateLimit-Used-Bytes", String.valueOf(result.usedBytes()));
		response.setHeader("X-RateLimit-Usage-Percent", String.format(java.util.Locale.US, "%.1f", result.usagePercentage()));

		if (!result.allowed()) {
			long retryAfterSeconds = Math.max(1, result.waitTimeMs() / 1000);
			response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
			response.setHeader("X-RateLimit-Wait-Ms", String.valueOf(result.waitTimeMs()));
			response.setStatus(429);
			response.setContentType("application/json");
			response.getWriter().write(
				"{\"error\":\"rate_limit_exceeded\",\"retryAfterSeconds\":" + retryAfterSeconds + "}"
			);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private long calculateSize(HttpServletRequest request) throws IOException {
		if (request.getContentLengthLong() <= 0) {
			return 0;
		}

		CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
		byte[] body = cachedRequest.getCachedBody();

		if (body.length == 0) {
			return 0;
		}

		return PayloadCompressor.compressedSize(body);
	}

	private String resolveIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isBlank()) {
			return xForwardedFor.split(",")[0].trim();
		}
		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isBlank()) {
			return xRealIp;
		}
		return request.getRemoteAddr();
	}
}
