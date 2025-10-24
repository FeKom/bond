package github.fekom.bond.infrastructure.persistence;

import github.fekom.bond.algorithms.TokenBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rate_limiter")
public class RateLimiter {
	@Id
	private String id;
	@Column(name = "client_id")
	private String clientId;
	@Column(name = "end_point")
	private String endPoint;
	private TokenBucket bucket;
	@Column(name = "created_at")
	private String createAt;
	@Column(name = "updated_at")
	private String updateAt;

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

	public TokenBucket getBucket() {
		return bucket;
	}

	public void setBucket(TokenBucket bucket) {
		this.bucket = bucket;
	}

	public String getCreateAt() {
		return createAt;
	}

	public void setCreateAt(String createdAt) {
		this.createAt = createdAt;
	}

	public String getUpdateAt() {
		return updateAt;
	}

	public void setUpdateAt(String updateAt) {
		this.updateAt = updateAt;
	}

	public static RateLimiter fromDomain(github.fekom.bond.domain.entities.RateLimiter.RateLimiter domain) {
		var entity = new RateLimiter();
		entity.setId(domain.id());
		entity.setClientId(domain.clientId());
		entity.setEndPoint(domain.endPoint());
		entity.setBucket(domain.bucket());
		entity.setCreateAt(domain.createAt());
		entity.setUpdateAt(domain.updateAt());
		return entity;
	}

	public github.fekom.bond.domain.entities.RateLimiter.RateLimiter toDomain() {
		return new github.fekom.bond.domain.entities.RateLimiter.RateLimiter(
				getId(),
				getClientId(),
				getEndPoint(),
				getBucket(),
				getCreateAt(),
				getUpdateAt());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((endPoint == null) ? 0 : endPoint.hashCode());
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((createAt == null) ? 0 : createAt.hashCode());
		result = prime * result + ((updateAt == null) ? 0 : updateAt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RateLimiter other = (RateLimiter) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (clientId == null) {
			if (other.clientId != null)
				return false;
		} else if (!clientId.equals(other.clientId))
			return false;
		if (endPoint == null) {
			if (other.endPoint != null)
				return false;
		} else if (!endPoint.equals(other.endPoint))
			return false;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (createAt == null) {
			if (other.createAt != null)
				return false;
		} else if (!createAt.equals(other.createAt))
			return false;
		if (updateAt == null) {
			if (other.updateAt != null)
				return false;
		} else if (!updateAt.equals(other.updateAt))
			return false;
		return true;
	}
}
