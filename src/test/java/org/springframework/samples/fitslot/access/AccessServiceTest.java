package org.springframework.samples.fitslot.access;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberService;
import org.springframework.samples.fitslot.member.MemberStatus;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificate;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificateService;
import org.springframework.samples.fitslot.membership.MemberSubscription;
import org.springframework.samples.fitslot.membership.MemberSubscriptionRepository;
import org.springframework.samples.fitslot.membership.MembershipTier;
import org.springframework.samples.fitslot.membership.ServiceType;
import org.springframework.samples.fitslot.membership.SubscriptionStatus;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBandConfiguration;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBandConfigurationRepository;
import org.springframework.samples.fitslot.payment.PaymentRecordRepository;
import org.springframework.samples.fitslot.payment.PaymentStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock private AccessRecordRepository accessRecordRepository;
    @Mock private MemberService memberService;
    @Mock private MedicalCertificateService certificateService;
    @Mock private MemberSubscriptionRepository subscriptionRepository;
    @Mock private PaymentRecordRepository paymentRepository;
    @Mock private TimeBandConfigurationRepository timeBandRepository;

    // Fixed time makes tests independent from the day they run.
    private final Clock clock = Clock.fixed(
        Instant.parse("2026-11-21T09:00:00Z"), ZoneId.of("Europe/Madrid"));

    private AccessService accessService;

    private final Integer memberId = 9;
    private Member member;
    private MedicalCertificate certificate;

    @BeforeEach
    void setUp() {
        // Constructor from the original AccessService supplied in the chat.
        // If the current service has extra constructor dependencies,
        // add matching mocks and pass them here.
        accessService = new AccessService(
            accessRecordRepository, memberService, certificateService,
            subscriptionRepository, paymentRepository, timeBandRepository, clock);

        member = new Member();
        member.setId(memberId);
        member.setStatus(MemberStatus.ACTIVE);
        certificate = new MedicalCertificate();
        certificate.setExpiryDate(LocalDate.of(2027, 1, 1));
    }

    private void validMemberAndCertificate() {
        when(memberService.findById(memberId)).thenReturn(member);
        when(certificateService.findLatest(memberId)).thenReturn(certificate);
        when(certificateService.isValid(certificate)).thenReturn(true);
    }

    private MemberSubscription subscription(Set<ServiceType> services) {
        MembershipTier tier = new MembershipTier();
        tier.setAllowedServices(services);
        tier.setEntryQuota(null);
        tier.setTimeBand(TimeBand.FULL_DAY);
        MemberSubscription subscription = new MemberSubscription();
        subscription.setId(9);
        subscription.setTier(tier);
        subscription.setStartDate(LocalDate.of(2026, 11, 1));
        subscription.setEndDate(LocalDate.of(2026, 11, 30));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }

    private void activeSubscriptions(MemberSubscription... subscriptions) {
        when(subscriptionRepository.findActiveForCheckIn(
            memberId, SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscriptions));
    }

    private void configureTimeBand(LocalTime start, LocalTime end) {
        TimeBandConfiguration configuration = new TimeBandConfiguration();
        configuration.setTimeBand(TimeBand.FULL_DAY);
        configuration.setStartTime(start);
        configuration.setEndTime(end);
        when(timeBandRepository.findByTimeBand(TimeBand.FULL_DAY))
            .thenReturn(java.util.Optional.of(configuration));
    }

    @Test
    void missingMemberDoesNotCreateAccessRecord() {
        when(memberService.findById(memberId)).thenThrow(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void missingCertificateReturnsForbidden() {
        when(memberService.findById(memberId)).thenReturn(member);
        when(certificateService.findLatest(memberId)).thenReturn(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void invalidCertificateReturnsForbidden() {
        when(memberService.findById(memberId)).thenReturn(member);
        when(certificateService.findLatest(memberId)).thenReturn(certificate);
        when(certificateService.isValid(certificate)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void noActiveSubscriptionReturnsForbidden() {
        validMemberAndCertificate();
        // Mockito returns an empty list for an unstubbed List repository method.
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void serviceNotIncludedReturnsForbidden() {
        validMemberAndCertificate();
        activeSubscriptions(subscription(Set.of(ServiceType.POOL)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void unpaidSubscriptionReturnsForbidden() {
        validMemberAndCertificate();
        activeSubscriptions(subscription(Set.of(ServiceType.WEIGHT_ROOM)));
        // Mockito defaults to false for the unpaid subscription.
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void allowsCheckInAtTheInclusiveStartOfThePermittedTimeBand() {
        validMemberAndCertificate();
        activeSubscriptions(subscription(Set.of(ServiceType.WEIGHT_ROOM)));
        when(paymentRepository.existsBySubscription_IdAndStatus(9, PaymentStatus.PAID))
            .thenReturn(true);
        configureTimeBand(LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(accessRecordRepository.save(any(AccessRecord.class))).thenAnswer(invocation -> {
            AccessRecord record = invocation.getArgument(0);
            record.setId(42);
            return record;
        });

        CheckInResponse response = accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM);

        assertEquals("ALLOWED", response.result());
        assertEquals(42, response.accessRecordId());
        verify(accessRecordRepository).save(any(AccessRecord.class));
    }

    @Test
    void deniesCheckInAtTheExclusiveEndOfThePermittedTimeBand() {
        validMemberAndCertificate();
        activeSubscriptions(subscription(Set.of(ServiceType.WEIGHT_ROOM)));
        when(paymentRepository.existsBySubscription_IdAndStatus(9, PaymentStatus.PAID))
            .thenReturn(true);
        configureTimeBand(LocalTime.of(9, 0), LocalTime.of(10, 0));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("Access denied: Access outside the allowed time band", error.getReason());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void allowsCheckInDuringAnOvernightTimeBand() {
        validMemberAndCertificate();
        activeSubscriptions(subscription(Set.of(ServiceType.WEIGHT_ROOM)));
        when(paymentRepository.existsBySubscription_IdAndStatus(9, PaymentStatus.PAID))
            .thenReturn(true);
        configureTimeBand(LocalTime.of(22, 0), LocalTime.of(11, 0));
        when(accessRecordRepository.save(any(AccessRecord.class))).thenAnswer(invocation -> {
            AccessRecord record = invocation.getArgument(0);
            record.setId(43);
            return record;
        });

        CheckInResponse response = accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM);

        assertEquals("ALLOWED", response.result());
        assertEquals(43, response.accessRecordId());
    }

    @Test
    void reportsRestrictedScheduleAndReasonForTheServerCurrentTime() {
        when(memberService.findById(memberId)).thenReturn(member);
        when(certificateService.findLatest(memberId)).thenReturn(null);
        MemberSubscription currentSubscription = subscription(Set.of(ServiceType.WEIGHT_ROOM));
        when(subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId))
            .thenReturn(List.of(currentSubscription));
        when(paymentRepository.existsBySubscription_IdAndStatus(9, PaymentStatus.PAID))
            .thenReturn(true);
        configureTimeBand(LocalTime.of(10, 1), LocalTime.of(11, 0));

        CheckInInfoResponse response = accessService.getCheckInInfo(memberId);

        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 0), response.checkedAt());
        assertFalse(response.subscriptions().get(0).timeSlotAllowed());
        assertEquals(
            "Access outside the allowed time band",
            response.subscriptions().get(0).timeSlotDenialReason());
    }

    @Test
    void reportsTimeBandIndependentlyFromFrozenSubscriptionStatus() {
        when(memberService.findById(memberId)).thenReturn(member);
        MemberSubscription frozenSubscription =
                subscription(Set.of(ServiceType.WEIGHT_ROOM));
        frozenSubscription.setStatus(SubscriptionStatus.FROZEN);
        when(subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId))
                .thenReturn(List.of(frozenSubscription));
        configureTimeBand(LocalTime.of(8, 0), LocalTime.of(20, 0));

        CheckInInfoResponse response = accessService.getCheckInInfo(memberId);
        CheckInInfoResponse.SubscriptionInfo info = response.subscriptions().get(0);

        assertEquals(SubscriptionStatus.FROZEN, info.verificationStatus());
        assertEquals("Subscription is frozen", info.denialReason());
        assertTrue(info.timeSlotAllowed());
        assertNull(info.timeSlotDenialReason());
    }

    @Test
    void frozenSubscriptionStillCannotCheckInWhenItsTimeBandAllowsAccess() {
        validMemberAndCertificate();
        MemberSubscription frozenSubscription =
                subscription(Set.of(ServiceType.WEIGHT_ROOM));
        frozenSubscription.setStatus(SubscriptionStatus.FROZEN);
        activeSubscriptions();
        when(subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId))
                .thenReturn(List.of(frozenSubscription));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> accessService.checkIn(memberId, ServiceType.WEIGHT_ROOM));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("Access denied: Subscription is frozen", error.getReason());
        verify(accessRecordRepository, never()).save(any());
    }

    @Test
    void validatesAnIncludedServiceWithoutConsumingAnEntryOrCreatingAnAccessRecord() {
        validMemberAndCertificate();
        MemberSubscription entrySubscription = subscription(Set.of(ServiceType.WEIGHT_ROOM));
        entrySubscription.getTier().setEntryQuota(10);
        entrySubscription.setRemainingEntries(4);
        when(subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId))
            .thenReturn(List.of(entrySubscription));
        when(paymentRepository.existsBySubscription_IdAndStatus(9, PaymentStatus.PAID))
            .thenReturn(true);
        configureTimeBand(LocalTime.of(9, 0), LocalTime.of(11, 0));

        AccessValidationResponse response =
            accessService.validateAccess(memberId, ServiceType.WEIGHT_ROOM);

        assertTrue(response.authorized());
        assertNull(response.reason());
        assertEquals(4, entrySubscription.getRemainingEntries());
        verify(accessRecordRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void returnsTheReasonWhenTheRequestedServiceIsNotIncluded() {
        validMemberAndCertificate();
        when(subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId))
            .thenReturn(List.of(subscription(Set.of(ServiceType.POOL))));

        AccessValidationResponse response =
            accessService.validateAccess(memberId, ServiceType.WEIGHT_ROOM);

        assertFalse(response.authorized());
        assertEquals("Subscription does not include this service", response.reason());
        verify(accessRecordRepository, never()).save(any());
    }
}
