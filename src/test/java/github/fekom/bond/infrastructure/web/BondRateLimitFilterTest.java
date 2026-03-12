package github.fekom.bond.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import github.fekom.bond.api.RateLimiterService;
import github.fekom.bond.domain.RequestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("BondRateLimitFilter")
class BondRateLimitFilterTest {

	@Mock
	private RateLimiterService rateLimiterService;

	private BondRateLimitFilter filter;

	@BeforeEach
	void setUp() {
		filter = new BondRateLimitFilter(rateLimiterService);
	}

	@Nested
	@DisplayName("doFilterInternal")
	class DoFilterInternal {

		@Test
		@DisplayName("should allow request and continue filter chain")
		void shouldAllowRequestAndContinueChain() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
			request.setRemoteAddr("10.0.0.1");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			when(rateLimiterService.checkRateLimit(eq("10.0.0.1"), eq("/api/data"), anyLong()))
				.thenReturn(new RequestResult(true, false, 0, 0.0, 0));

			filter.doFilterInternal(request, response, chain);

			assertEquals(200, response.getStatus());
			assertNotNull(chain.getRequest()); // chain was called
		}

		@Test
		@DisplayName("should block request with 429 when rate limit exceeded")
		void shouldBlockRequestWith429WhenRateLimitExceeded() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
			request.setRemoteAddr("10.0.0.1");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			when(rateLimiterService.checkRateLimit(eq("10.0.0.1"), eq("/api/upload"), anyLong()))
				.thenReturn(new RequestResult(false, false, 32_000, 97.6, 11_000));

			filter.doFilterInternal(request, response, chain);

			assertEquals(429, response.getStatus());
			assertEquals("11", response.getHeader("Retry-After"));
			assertEquals("11000", response.getHeader("X-RateLimit-Wait-Ms"));
			assertNull(chain.getRequest()); // chain was NOT called
		}

		@Test
		@DisplayName("should resolve IP from X-Forwarded-For")
		void shouldResolveIpFromXForwardedFor() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
			request.setRemoteAddr("127.0.0.1");
			request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			when(rateLimiterService.checkRateLimit(eq("203.0.113.50"), eq("/api/data"), anyLong()))
				.thenReturn(new RequestResult(true, false, 0, 0.0, 0));

			filter.doFilterInternal(request, response, chain);

			// Verify it used the first IP from X-Forwarded-For
			ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
			verify(rateLimiterService).checkRateLimit(ipCaptor.capture(), eq("/api/data"), anyLong());
			assertEquals("203.0.113.50", ipCaptor.getValue());
		}

		@Test
		@DisplayName("should resolve IP from X-Real-IP when X-Forwarded-For is absent")
		void shouldResolveIpFromXRealIp() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
			request.setRemoteAddr("127.0.0.1");
			request.addHeader("X-Real-IP", "198.51.100.25");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			when(rateLimiterService.checkRateLimit(eq("198.51.100.25"), eq("/api/data"), anyLong()))
				.thenReturn(new RequestResult(true, false, 0, 0.0, 0));

			filter.doFilterInternal(request, response, chain);

			ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
			verify(rateLimiterService).checkRateLimit(ipCaptor.capture(), eq("/api/data"), anyLong());
			assertEquals("198.51.100.25", ipCaptor.getValue());
		}

		@Test
		@DisplayName("should include rate limit headers on allowed response")
		void shouldIncludeRateLimitHeadersOnAllowedResponse() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
			request.setRemoteAddr("10.0.0.1");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			when(rateLimiterService.checkRateLimit(eq("10.0.0.1"), eq("/api/data"), anyLong()))
				.thenReturn(new RequestResult(true, false, 500, 1.5, 0));

			filter.doFilterInternal(request, response, chain);

			assertEquals("500", response.getHeader("X-RateLimit-Used-Bytes"));
			assertEquals("1.5", response.getHeader("X-RateLimit-Usage-Percent"));
		}

		@Test
		@DisplayName("should use size 0 for GET without body")
		void shouldUseZeroSizeForGetWithoutBody() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
			request.setRemoteAddr("10.0.0.1");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			when(rateLimiterService.checkRateLimit(eq("10.0.0.1"), eq("/api/data"), eq(0L)))
				.thenReturn(new RequestResult(true, false, 0, 0.0, 0));

			filter.doFilterInternal(request, response, chain);

			verify(rateLimiterService).checkRateLimit("10.0.0.1", "/api/data", 0L);
		}

		@Test
		@DisplayName("should return 403 when IP is blocked")
		void shouldReturn403WhenIpIsBlocked() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
			request.setRemoteAddr("10.0.0.1");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			when(rateLimiterService.checkRateLimit(eq("10.0.0.1"), eq("/api/data"), anyLong()))
				.thenReturn(RequestResult.blockedResult());

			filter.doFilterInternal(request, response, chain);

			assertEquals(403, response.getStatus());
			assertEquals("application/json", response.getContentType());
			assertTrue(response.getContentAsString().contains("ip_blocked"));
			assertNull(chain.getRequest()); // chain was NOT called
		}
	}
}
