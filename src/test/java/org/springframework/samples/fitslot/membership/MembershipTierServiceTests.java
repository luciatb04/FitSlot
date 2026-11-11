package org.springframework.samples.fitslot.membership;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBandConfigurationRepository;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MembershipTierServiceTests {

    private static final Integer TIER_ID = 1;

    @Mock
    private MembershipTierRepository repository;

    @Mock
    private TimeBandConfigurationRepository timeBandRepository;

    private MembershipTierService service;
    private MembershipTier tier;

    @BeforeEach
    void setUp() {
        service = new MembershipTierService(repository, timeBandRepository);
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
    void shouldCreateTierFromRequestAndCopyServices() {
        Set<ServiceType> services = new HashSet<>(Set.of(ServiceType.WEIGHT_ROOM, ServiceType.POOL));
        MembershipTierRequest request = new MembershipTierRequest(
                "  Premium  ",
                new BigDecimal("49.99"),
                30,
                null,
                services,
                TimeBand.FULL_DAY);
        when(repository.findByNameIgnoreCase("Premium")).thenReturn(Optional.empty());
        when(repository.save(any(MembershipTier.class))).thenAnswer(invocation -> {
            MembershipTier created = invocation.getArgument(0);
            created.setId(2);
            return created;
        });

        MembershipTier created = service.createTier(request);

        assertThat(created.getId()).isEqualTo(2);
        assertThat(created.getName()).isEqualTo("Premium");
        assertThat(created.getPrice()).isEqualByComparingTo("49.99");
        assertThat(created.getDurationDays()).isEqualTo(30);
        assertThat(created.getEntryQuota()).isNull();
        assertThat(created.getAllowedServices()).containsExactlyInAnyOrder(
                ServiceType.WEIGHT_ROOM, ServiceType.POOL);
        assertThat(created.getAllowedServices()).isNotSameAs(services);
        assertThat(created.getTimeBand()).isEqualTo(TimeBand.FULL_DAY);
        verify(repository).findByNameIgnoreCase("Premium");
        verify(repository).save(created);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldCreateEntryBasedTierWithoutDuration() {
        MembershipTierRequest request = new MembershipTierRequest(
                "Ten entries",
                new BigDecimal("25.00"),
                null,
                10,
                Set.of(ServiceType.CLASSES),
                TimeBand.MORNINGS_ONLY);
        when(repository.findByNameIgnoreCase("Ten entries")).thenReturn(Optional.empty());
        when(repository.save(any(MembershipTier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MembershipTier created = service.createTier(request);

        assertThat(created.getDurationDays()).isNull();
        assertThat(created.getEntryQuota()).isEqualTo(10);
        assertThat(created.getAllowedServices()).containsExactly(ServiceType.CLASSES);
        verify(repository).findByNameIgnoreCase("Ten entries");
        verify(repository).save(created);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldRejectDuplicateTierName() {
        MembershipTierRequest request = new MembershipTierRequest(
                "Premium",
                new BigDecimal("49.99"),
                30,
                null,
                Set.of(ServiceType.POOL),
                TimeBand.FULL_DAY);
        when(repository.findByNameIgnoreCase("Premium")).thenReturn(Optional.of(tier));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTier(request));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).contains("already exists");
        verify(repository).findByNameIgnoreCase("Premium");
        verifyNoMoreInteractions(repository);
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

    @Test
    void shouldRejectEmptyServicesWithoutChangingTier() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateServices(TIER_ID, Set.of()));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("At least one service is required");
        assertThat(tier.getAllowedServices()).containsExactly(ServiceType.WEIGHT_ROOM);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldRejectNullServicesWithoutAccessingRepository() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateServices(TIER_ID, null));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("At least one service is required");
        verifyNoMoreInteractions(repository);
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
