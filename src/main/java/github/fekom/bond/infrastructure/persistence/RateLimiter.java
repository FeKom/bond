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
@Table(name = "rate_limiters")
public class RateLimiter {

	@Id
	private String id;

	@Column(name = "client_id")
	private String clientId;

	@Column(name = "end_point")
	private String endPoint;

	@JdbcTypeCode(SqlTypes.JSON)
	private TokenBucket bucket;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return clientId;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

	public void setBucket(TokenBucket bucket) {
		this.bucket = bucket;
	}

	public TokenBucket getBucket() {
		return bucket;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getupdatedAt() {
		return updatedAt;
	}

	public void setupdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public static RateLimiter fromDomain(github.fekom.bond.domain.entities.RateLimiter.RateLimiter domain) {
		var entity = new RateLimiter();
		entity.setId(domain.id());
		entity.setClientId(domain.clientId());
		entity.setEndPoint(domain.endPoint());
		entity.setCreatedAt(domain.createdAt());
		entity.setBucket(domain.bucket());
		entity.setupdatedAt(domain.updatedAt());
		return entity;
	}

	public github.fekom.bond.domain.entities.RateLimiter.RateLimiter toDomain() {
		return new github.fekom.bond.domain.entities.RateLimiter.RateLimiter(
			getId(),
			getClientId(),
			getEndPoint(),
			getBucket(),
			getCreatedAt(),
			getupdatedAt()
		);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((endPoint == null) ? 0 : endPoint.hashCode());
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
		RateLimiter other = (RateLimiter) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		if (clientId == null) {
			if (other.clientId != null) return false;
		} else if (!clientId.equals(other.clientId)) return false;
		if (endPoint == null) {
			if (other.endPoint != null) return false;
		} else if (!endPoint.equals(other.endPoint)) return false;
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
