package github.fekom.bond.infrastructure.persistence;

import github.fekom.bond.algorithms.TokenBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "requests")
public class Request {

	@Id
	private String id;

	@Column(name = "ip_address", nullable = false, length = 45)
	private String ipAddress;

	@Column(name = "endpoint", nullable = false, length = 1024)
	private String endpoint;

	@JdbcTypeCode(SqlTypes.JSON)
	private TokenBucket bucket;

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

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public TokenBucket getBucket() {
		return bucket;
	}

	public void setBucket(TokenBucket bucket) {
		this.bucket = bucket;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Request other = (Request) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		if (ipAddress == null) {
			if (other.ipAddress != null) return false;
		} else if (!ipAddress.equals(other.ipAddress)) return false;
		if (endpoint == null) {
			if (other.endpoint != null) return false;
		} else if (!endpoint.equals(other.endpoint)) return false;
		if (bucket == null) {
			if (other.bucket != null) return false;
		} else if (!bucket.equals(other.bucket)) return false;
		if (createdAt == null) {
			if (other.createdAt != null) return false;
		} else if (!createdAt.equals(other.createdAt)) return false;
		if (updatedAt == null) {
			if (other.updatedAt != null) return false;
		} else if (!updatedAt.equals(other.updatedAt)) return false;
		return true;
	}
}
