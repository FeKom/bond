package github.fekom.bond.domain.entities.RateLimiter;

public record RateLimiterResult(
    boolean allowed,           // Permitido ou bloqueado?
    long usedBytes,            // Bytes consumidos
    double usagePercentage,    // Percentual de uso (0-100)
    long waitTimeMs            // Tempo de espera em ms (0 se permitido)
) { }
