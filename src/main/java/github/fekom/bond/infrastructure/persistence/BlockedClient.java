package github.fekom.bond.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_clients")
public class BlockedClient {

	@Id
	private String id;

	@Column(name = "ip_address", nullable = false, unique = true, length = 45)
	private String ipAddress;

	@Column(name = "reason", length = 500)
	private String reason;

	@Column(name = "blocked_at", nullable = false)
	private LocalDateTime blockedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDateTime getBlockedAt() {
		return blockedAt;
	}

	public void setBlockedAt(LocalDateTime blockedAt) {
		this.blockedAt = blockedAt;
	}
}
