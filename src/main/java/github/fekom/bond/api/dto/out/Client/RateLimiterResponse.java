package github.fekom.bond.api.dto.out.Client;

public record RateLimiterResponse(
    boolean allowed,           // Permitido ou bloqueado?
    long usedBytes,            // Bytes consumidos
    double usagePercentage,    // Percentual de uso (0-100)
    long waitTimeMs            // Tempo de espera em ms (0 se permitido)
) { }
