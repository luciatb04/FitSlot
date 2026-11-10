package org.springframework.samples.fitslot.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class MembershipTierRepositoryTests {

    @Autowired
    private MembershipTierRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsTierAndAllowedServices() {
        // Crea un plan por duración con dos servicios permitidos.
        MembershipTier tier = new MembershipTier();
        tier.setName("Plan mensual de prueba");
        tier.setPrice(new BigDecimal("45.00"));
        tier.setDurationDays(30);
        tier.setEntryQuota(null);
        tier.setAllowedServices(Set.of(
            ServiceType.WEIGHT_ROOM,
            ServiceType.CLASSES
        ));
        tier.setTimeBand(TimeBand.FULL_DAY);

        MembershipTier saved = repository.save(tier);
        entityManager.flush();
        Integer savedId = saved.getId();

        // Fuerza la lectura desde la base, no solo desde la sesión actual.
        entityManager.clear();

        MembershipTier loaded = repository.findById(savedId).orElseThrow();

        assertEquals("Plan mensual de prueba", loaded.getName());
        assertEquals(new BigDecimal("45.00"), loaded.getPrice());
        assertEquals(30, loaded.getDurationDays());
        assertNull(loaded.getEntryQuota());
        assertEquals(
            Set.of(ServiceType.WEIGHT_ROOM, ServiceType.CLASSES),
            loaded.getAllowedServices()
        );
        assertEquals(TimeBand.FULL_DAY, loaded.getTimeBand());
    }
}