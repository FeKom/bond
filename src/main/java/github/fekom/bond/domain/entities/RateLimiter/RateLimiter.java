package github.fekom.bond.domain.entities.RateLimiter;

import com.fasterxml.uuid.Generators;
import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.domain.enums.TierType;
import java.time.LocalDateTime;
import java.util.UUID;

public record RateLimiter(
	String id,
	String clientId,
	String endPoint,
	TokenBucket bucket,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static RateLimiter create(String clientId, String endPoint, TierType tier) {
		UUID uuid = Generators.timeBasedEpochGenerator().generate();
		return new RateLimiter(
			uuid.toString(),
			clientId,
			endPoint,
			new TokenBucket(tier),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
	}

	public RateLimiter withUpdateTier(TierType tier) {
		return new RateLimiter(
			this.id,
			this.clientId,
			this.endPoint,
			new TokenBucket(tier),
			this.createdAt,
			LocalDateTime.now()
		);
	}
}
