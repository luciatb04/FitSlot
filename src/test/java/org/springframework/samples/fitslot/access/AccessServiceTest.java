package org.springframework.samples.fitslot.access;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            subscriptionRepository, paymentRepository, null, clock);

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
}
