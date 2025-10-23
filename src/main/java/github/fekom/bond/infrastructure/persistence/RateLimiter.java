package github.fekom.bond.infrastructure.persistence;

import github.fekom.bond.algorithms.TokenBucket;
import github.fekom.bond.domain.enums.TierType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rate_limiter")
public class RateLimiter {
    @Id
    private String id;
    private String clientId;
    private String endPoint;
    private TokenBucket bucket;
    private TierType tier;
    private String createdAt;

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
    public TierType getTier() {
        return tier;
    }
    public void setTier(TierType tier) {
        this.tier = tier;
    }
    public void setBucket(TokenBucket bucket) {
        this.bucket = bucket;
    }
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public static RateLimiter fromDomain(github.fekom.bond.domain.entities.RateLimiter domain) {
        var entity = new RateLimiter();
        entity.setId(domain.id());
        entity.setClientId(domain.clientId());
        entity.setEndPoint(domain.endPoint());
        entity.setBucket(domain.bucket());
        entity.setCreatedAt(domain.createdAt());
        entity.setTier(domain.tier());
        return entity;
    }

    public github.fekom.bond.domain.entities.RateLimiter toDomain() {
        return new github.fekom.bond.domain.entities.RateLimiter(
            getId(),
            getClientId(),
            getEndPoint(),
            getBucket(),
            getCreatedAt(),
            getTier());
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + ((endPoint == null) ? 0 : endPoint.hashCode());
        result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
        result = prime * result + ((tier == null) ? 0 : tier.hashCode());
        result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
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
        if (tier != other.tier)
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        } else if (!createdAt.equals(other.createdAt))
            return false;
        return true;
    }
}
