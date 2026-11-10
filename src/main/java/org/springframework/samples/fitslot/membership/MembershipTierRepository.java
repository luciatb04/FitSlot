// membership/MembershipTierRepository.java
package org.springframework.samples.fitslot.membership;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface MembershipTierRepository
        extends CrudRepository<MembershipTier, Integer> {
    Optional<MembershipTier> findByNameIgnoreCase(String name);
}