// membership/MemberSubscriptionRepository.java
package org.springframework.samples.fitslot.membership;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface MemberSubscriptionRepository
        extends CrudRepository<MemberSubscription, Integer> {
    List<MemberSubscription> findByMember_IdOrderByStartDateDesc(Integer memberId);

    List<MemberSubscription> findByMember_IdAndStatusOrderByEndDateDesc(
            Integer memberId,
            SubscriptionStatus status);

    Optional<MemberSubscription> findFirstByMember_IdAndStatusOrderByEndDateDesc(
            Integer memberId,
            SubscriptionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM MemberSubscription s
            WHERE s.member.id = :memberId
            AND s.status = :status
            ORDER BY s.endDate DESC
            """)
    List<MemberSubscription> findActiveForCheckIn(
            @Param("memberId") Integer memberId,
            @Param("status") SubscriptionStatus status);
}