package github.fekom.bond.domain.entities.Client;

import java.util.Optional;
import java.util.Set;
import org.inferred.freebuilder.FreeBuilder;

import github.fekom.bond.domain.enums.TierType;

@FreeBuilder
public interface ClientQuery {
	Optional<Set<String>> ids();

	Optional<Set<TierType>> tiers();

	Optional<Boolean> enabled();

	Optional<String> createdAfter();

	Optional<String> createdBefore();

	Optional<String> updatedAfter();

	Optional<String> updatedBefore();

	Optional<Integer> limit();

	class Builder extends ClientQuery_Builder {
	}

	static Builder builder() {
		return new Builder();
	}
}
