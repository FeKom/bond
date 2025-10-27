package github.fekom.bond.api.dto.out.Client;

import github.fekom.bond.domain.enums.TierType;

public record CreateClientResponse(
	String id,
		boolean enabled,
		String createdAt,
		String updatedAt,
		TierType tier,
		String message
) {
	public static CreateClientResponse fromDomain(github.fekom.bond.domain.entities.Client.Client client, String message) {
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
