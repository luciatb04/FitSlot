package org.springframework.samples.fitslot.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MembershipTierServiceTests {

    private static final Integer TIER_ID = 1;

    @Mock
    private MembershipTierRepository repository;

    private MembershipTierService service;
    private MembershipTier tier;

    @BeforeEach
    void setUp() {
        service = new MembershipTierService(repository);
        tier = new MembershipTier();
        tier.setId(TIER_ID);
        tier.setAllowedServices(new HashSet<>(Set.of(ServiceType.WEIGHT_ROOM)));
    }

    @Test
    void shouldReturnCopyOfAllowedServices() {
        when(repository.findById(TIER_ID)).thenReturn(Optional.of(tier));

        Set<ServiceType> result = service.getServices(TIER_ID);

        assertThat(result).containsExactly(ServiceType.WEIGHT_ROOM);
        result.add(ServiceType.POOL);
        assertThat(tier.getAllowedServices()).containsExactly(ServiceType.WEIGHT_ROOM);
        verify(repository).findById(TIER_ID);
    }

    @Test
    void shouldReturnAllTiers() {
        List<MembershipTier> tiers = List.of(tier);
        when(repository.findAll()).thenReturn(tiers);

        assertThat(service.getAllTiers()).containsExactly(tier);

        verify(repository).findAll();
    }

    @Test
    void shouldReplaceServicesAndSaveTier() {
        Set<ServiceType> newServices = Set.of(ServiceType.CLASSES, ServiceType.POOL);
        when(repository.findById(TIER_ID)).thenReturn(Optional.of(tier));
        when(repository.save(tier)).thenReturn(tier);

        Set<ServiceType> result = service.updateServices(TIER_ID, newServices);

        assertThat(result).containsExactlyInAnyOrder(ServiceType.CLASSES, ServiceType.POOL);
        assertThat(tier.getAllowedServices())
                .containsExactlyInAnyOrder(ServiceType.CLASSES, ServiceType.POOL);
        verify(repository).findById(TIER_ID);
        verify(repository).save(tier);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowNotFoundWhenGettingServicesForUnknownTier() {
        when(repository.findById(TIER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.getServices(TIER_ID));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Membership tier not found");
        verify(repository).findById(TIER_ID);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingUnknownTier() {
        when(repository.findById(TIER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateServices(TIER_ID, Set.of(ServiceType.POOL)));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Membership tier not found");
        verify(repository).findById(TIER_ID);
    }

    @ParameterizedTest
    @EnumSource(TimeBand.class)
    void shouldAssignAndSaveEachTimeBand(TimeBand timeBand) {
        when(repository.findById(TIER_ID)).thenReturn(Optional.of(tier));
        when(repository.save(tier)).thenReturn(tier);

        TimeBand result = service.updateTimeBand(TIER_ID, timeBand);

        assertThat(result).isEqualTo(timeBand);
        assertThat(tier.getTimeBand()).isEqualTo(timeBand);
        verify(repository).findById(TIER_ID);
        verify(repository).save(tier);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnSavedTimeBand() {
        tier.setTimeBand(TimeBand.MORNINGS_ONLY);
        when(repository.findById(TIER_ID)).thenReturn(Optional.of(tier));

        assertThat(service.getTimeBand(TIER_ID)).isEqualTo(TimeBand.MORNINGS_ONLY);

        verify(repository).findById(TIER_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldRejectInvalidNullTimeBandBeforeAccessingRepository() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateTimeBand(TIER_ID, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Time band cannot be null");
        verifyNoMoreInteractions(repository);
    }
}
