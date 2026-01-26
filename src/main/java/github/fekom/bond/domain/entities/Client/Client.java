package github.fekom.bond.domain.entities.Client;

import com.fasterxml.uuid.Generators;
import github.fekom.bond.domain.enums.TierType;
import java.time.LocalDateTime;

public record Client(String id, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt, TierType tier) {
	public static Client create(TierType tier) {
		var uuid = Generators.timeBasedEpochGenerator().generate();
		LocalDateTime now = LocalDateTime.now();
		return new Client("API_KEY_" + uuid, true, now, now, tier);
	}
}
