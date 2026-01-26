package github.fekom.bond.infrastructure.persistence;

import github.fekom.bond.domain.enums.TierType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
public class Client {

	@Id
	private String id;

	@Enumerated(EnumType.STRING)
	private TierType tier;

	private boolean enabled = true;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TierType getTier() {
		return tier;
	}

	public void setTier(TierType tier) {
		this.tier = tier;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public static Client fromDomain(github.fekom.bond.domain.entities.Client.Client domain) {
		var client = new Client();
		client.id = domain.id();
		client.tier = domain.tier();
		client.enabled = domain.enabled();
		client.createdAt = domain.createdAt();
		client.updatedAt = domain.updatedAt();
		return client;
	}

	public github.fekom.bond.domain.entities.Client.Client toDomain() {
		return new github.fekom.bond.domain.entities.Client.Client(
			getId(),
			isEnabled(),
			getCreatedAt(),
			getUpdatedAt(),
			getTier()
		);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((tier == null) ? 0 : tier.hashCode());
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Client other = (Client) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		if (tier != other.tier) return false;
		if (enabled != other.enabled) return false;
		if (createdAt == null) {
			if (other.createdAt != null) return false;
		} else if (!createdAt.equals(other.createdAt)) return false;
		if (updatedAt == null) {
			if (other.updatedAt != null) return false;
		} else if (!updatedAt.equals(other.updatedAt)) return false;
		return true;
	}
}
