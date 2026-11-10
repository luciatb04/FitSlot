package org.springframework.samples.fitslot.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class MembershipTierTimeBandPersistenceTests {

    @Autowired
    private MembershipTierRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndReadBackTimeBandForMembershipTier() {
        MembershipTier tier = new MembershipTier();
        tier.setName("Time band persistence");
        tier.setPrice(BigDecimal.ZERO);
        tier.setTimeBand(TimeBand.MORNINGS_ONLY);

        MembershipTier saved = repository.save(tier);
        entityManager.flush();
        entityManager.clear();

        MembershipTier persisted = repository.findById(saved.getId()).orElseThrow();
        persisted.setTimeBand(TimeBand.EVENINGS_ONLY);
        entityManager.flush();
        entityManager.clear();

        MembershipTier reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getTimeBand()).isEqualTo(TimeBand.EVENINGS_ONLY);
    }
}
