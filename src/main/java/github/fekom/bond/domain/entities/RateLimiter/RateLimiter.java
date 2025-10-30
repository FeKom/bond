package github.fekom.bond.domain.entities.RateLimiter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.domain.enums.TierType;

public record RateLimiter(
		String id,
		String clientId,
		String endPoint,
		TokenBucket bucket,
		String createAt,
		String updateAt

) {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public static RateLimiter create(String clientId, String endPoint, TierType tier) {
		UUID uuid = Generators.timeBasedEpochGenerator().generate();
		String now = LocalDateTime.now().format(formatter);
		return new RateLimiter(
			uuid.toString(),
			clientId,
			endPoint,
			new TokenBucket(tier),
			now,
			now
			);
	}
	public RateLimiter withUpdateTier(TierType tier) {
		return new RateLimiter(
			this.id,
			this.clientId,
			this.endPoint,
			new TokenBucket(tier),
			this.createAt,
			LocalDateTime.now().format(formatter)
		);
	}

}
