package github.fekom.bond.domain;

/**
 * Result of a rate limit check.
 *
 * @param allowed whether the request was allowed
 * @param blocked whether the IP is blocked (when true, allowed is always false)
 * @param usedBytes total bytes consumed from the bucket
 * @param usagePercentage bucket usage as percentage (0-100)
 * @param waitTimeMs estimated wait time in milliseconds until enough capacity is available (0 if allowed)
 */
public record RequestResult(
		boolean allowed,
		boolean blocked,
		long usedBytes,
		double usagePercentage,
		long waitTimeMs
) {

	/** Creates a result for a blocked IP. */
	public static RequestResult blockedResult() {
		return new RequestResult(false, true, 0, 0, 0);
	}
}
