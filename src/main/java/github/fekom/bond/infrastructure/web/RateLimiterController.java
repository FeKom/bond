package github.fekom.bond.infrastructure.web;

import github.fekom.bond.api.RateLimiterService;
import github.fekom.bond.api.dto.out.Client.RateLimiterResponse;
import github.fekom.bond.domain.entities.RateLimiter.RateLimiterResult;
import github.fekom.bond.resolver.PayloadCompressor;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimiterController {

	private final RateLimiterService service;

	public RateLimiterController(RateLimiterService service) {
		this.service = service;
	}

	@PostMapping("/check")
	public ResponseEntity<?> check(@RequestHeader("X-API-Key") String clientId, @RequestBody String payload)
		throws IOException {
		byte[] compressed = PayloadCompressor.compress(payload);
		long sizeBytes = compressed.length;

		RateLimiterResult result = service.checkRateLimit(clientId, sizeBytes);

		if (result.allowed()) {
			return ResponseEntity.ok()
				.header("X-RateLimit-Used", String.valueOf(result.usedBytes()))
				.header("X-RateLimit-Usage", String.format("%.2f%%", result.usagePercentage()))
				.body(new RateLimiterResponse(true, result.usagePercentage(), 0));
		} else {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header("X-RateLimit-Reset-After", String.valueOf(result.waitTimeMs()))
				.body(new RateLimiterResponse(false, result.usagePercentage(), result.waitTimeMs()));
		}
	}
}
