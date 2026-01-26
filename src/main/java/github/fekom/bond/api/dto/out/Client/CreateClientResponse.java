package github.fekom.bond.api.dto.out.Client;

import github.fekom.bond.domain.enums.TierType;
import java.time.LocalDateTime;

public record CreateClientResponse(
	String id,
	boolean enabled,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	TierType tier,
	String message
) {
	public static CreateClientResponse fromDomain(
		github.fekom.bond.domain.entities.Client.Client client,
		String message
	) {
		return new CreateClientResponse(
			client.id(),
			client.enabled(),
			client.createdAt(),
			client.updatedAt(),
			client.tier(),
			message
		);
	}
}
