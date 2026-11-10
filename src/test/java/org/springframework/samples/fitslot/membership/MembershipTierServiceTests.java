package org.springframework.samples.fitslot.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.exceptions.ResourceNotFoundException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)

/*
Esta prueba cubre dos planes válidos, los casos inválidos de modalidad, 
un nombre duplicado y un ID inexistente
*/
class MembershipTierServiceTests {

    @Mock
    private MembershipTierRepository repository;

    @InjectMocks
    private MembershipTierService service;

    @Test
    void createsDurationBasedTier() {
        MembershipTierRequest request = request(
            "Mensual",
            30,
            null
        );

        when(repository.existsByNameIgnoreCase("Mensual")).thenReturn(false);
        when(repository.save(any(MembershipTier.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        MembershipTier created = service.create(request);

        assertEquals("Mensual", created.getName());
        assertEquals(new BigDecimal("45.00"), created.getPrice());
        assertEquals(30, created.getDurationDays());
        assertEquals(null, created.getEntryQuota());
        assertEquals(Set.of(ServiceType.WEIGHT_ROOM, ServiceType.CLASSES),
            created.getAllowedServices());

        verify(repository).save(any(MembershipTier.class));
    }

    @Test
    void createsEntryBasedTier() {
        MembershipTierRequest request = request(
            "Bono 10 entradas",
            null,
            10
        );

        when(repository.existsByNameIgnoreCase("Bono 10 entradas"))
            .thenReturn(false);
        when(repository.save(any(MembershipTier.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        MembershipTier created = service.create(request);

        assertEquals(null, created.getDurationDays());
        assertEquals(10, created.getEntryQuota());
        verify(repository).save(any(MembershipTier.class));
    }

    @Test
    void rejectsTierWithBothDurationAndEntryQuota() {
        MembershipTierRequest request = request(
            "Plan inválido",
            30,
            10
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(repository, never()).save(any(MembershipTier.class));
    }

    @Test
    void rejectsTierWithoutDurationOrEntryQuota() {
        MembershipTierRequest request = request(
            "Plan inválido",
            null,
            null
        );

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(repository, never()).save(any(MembershipTier.class));
    }

    @Test
    void rejectsDuplicateName() {
        MembershipTierRequest request = request(
            "Mensual",
            30,
            null
        );

        when(repository.existsByNameIgnoreCase("Mensual")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any(MembershipTier.class));
    }

    @Test
    void rejectsUnknownTierId() {
        when(repository.findById(999)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(999));
    }

    private MembershipTierRequest request(
        String name,
        Integer durationDays,
        Integer entryQuota
    ) {
        return new MembershipTierRequest(
            name,
            new BigDecimal("45.00"),
            durationDays,
            entryQuota,
            Set.of(ServiceType.WEIGHT_ROOM, ServiceType.CLASSES),
            TimeBand.FULL_DAY
        );
    }
}