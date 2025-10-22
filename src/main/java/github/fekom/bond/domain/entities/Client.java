package github.fekom.bond.domain.entities;

//import github.fekom.bond.domain.enums.TierType;

public record Client(
    String id, 
    //TierType TierType,
    boolean enabled,
    String createdAt,
    String updatedAt
) {
}
