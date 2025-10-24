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

	public boolean isAllowed(long requestSizeBytes) {
		return bucket.allowRequest(requestSizeBytes);
	}

	public long getWaitTimeMs(long requestSizeBytes) {
		return bucket.getWaitTime(requestSizeBytes);
	}

	public long usedBytes() {
		return bucket.getUsedBytes();
	}

	public double usagePercentage() {
		return bucket.getUsagePercentage();
	}

	public static RateLimiter create(String clientId, String endPoint, TierType tier) {
		UUID uuid = Generators.timeBasedEpochGenerator().generate();
		String now = LocalDateTime.now().format(formatter);
		var bucket = new TokenBucket(tier);
		return new RateLimiter(uuid.toString(), clientId, endPoint, bucket, now, now);
	}

}
