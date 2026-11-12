// membership/MemberSubscriptionRepository.java
package org.springframework.samples.fitslot.membership;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface MemberSubscriptionRepository
        extends CrudRepository<MemberSubscription, Integer> {
    List<MemberSubscription> findByMember_IdOrderByStartDateDesc(Integer memberId);

    List<MemberSubscription> findByMember_IdAndStatusOrderByEndDateDesc(
        Integer memberId,
        SubscriptionStatus status
    );

    Optional<MemberSubscription> findFirstByMember_IdAndStatusOrderByEndDateDesc(
        Integer memberId,
        SubscriptionStatus status
    );
}