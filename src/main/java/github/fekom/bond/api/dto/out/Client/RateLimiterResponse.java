package github.fekom.bond.api.dto.out.Client;

public record RateLimiterResponse(
    boolean allowed,
    double usagePercentage,
    long waitTimeMs
) { }
