package github.fekom.bond.domain.entities;

import github.fekom.bond.domain.enums.TierType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.uuid.Generators;

public record Client(
		String id,
		boolean enabled,
		String createdAt,
		String updatedAt,
		TierType tier) {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public static Client create(TierType tier) {
		var uuid = Generators.timeBasedEpochGenerator().generate();
		String now = LocalDateTime.now().format(formatter);
		return new Client("API_KEY_" + uuid, true, now, now, tier);
	}
}
