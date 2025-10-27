package github.fekom.bond.api.dto.in.Client;

import github.fekom.bond.domain.enums.TierType;
import github.fekom.bond.domain.entities.Client.Client;
import jakarta.validation.constraints.NotNull;


public record CreateClientRequest(
	@NotNull TierType tier

	) {

	public Client toDomain() {
		return Client.create(tier);
	}
}
