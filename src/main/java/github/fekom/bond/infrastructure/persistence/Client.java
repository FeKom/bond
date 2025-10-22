package github.fekom.bond.infrastructure.persistence;



import github.fekom.bond.domain.enums.TierType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client {
    @Id 
    private String id; // IP 
    
    @Enumerated(EnumType.STRING)
    private TierType tier;
    private boolean enabled = true;
    private String createdAt;
    private String updatedAt;
    
}