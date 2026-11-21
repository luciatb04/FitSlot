package org.springframework.samples.fitslot.access;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberService;
import org.springframework.samples.fitslot.member.MemberStatus;
import org.springframework.samples.fitslot.medicalcertificate.CertificateValidity;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificate;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificateService;
import org.springframework.samples.fitslot.membership.MemberSubscription;
import org.springframework.samples.fitslot.membership.MemberSubscriptionRepository;
import org.springframework.samples.fitslot.membership.MembershipTier;
import org.springframework.samples.fitslot.membership.ServiceType;
import org.springframework.samples.fitslot.membership.SubscriptionStatus;
import org.springframework.samples.fitslot.payment.PaymentRecordRepository;
import org.springframework.samples.fitslot.payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBandConfiguration;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBandConfigurationRepository;

@Service
public class AccessService {

    private final AccessRecordRepository accessRecordRepository;
    private final MemberService memberService;
    private final MedicalCertificateService certificateService;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRepository;
    private final TimeBandConfigurationRepository timeBandRepository;
    private final Clock clock;

    public AccessService(
            AccessRecordRepository accessRecordRepository,
            MemberService memberService,
            MedicalCertificateService certificateService,
            MemberSubscriptionRepository subscriptionRepository,
            PaymentRecordRepository paymentRepository,
            TimeBandConfigurationRepository timeBandRepository,
            Clock clock) {

        this.accessRecordRepository = accessRecordRepository;
        this.memberService = memberService;
        this.certificateService = certificateService;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.timeBandRepository = timeBandRepository;
        this.clock = clock;
    }

    @Transactional
    public CheckInResponse checkIn(
            Integer memberId,
            ServiceType serviceAccessed) {

        Member member = memberService.findById(memberId);

        if (member.getStatus() != MemberStatus.ACTIVE) {
            deny("Member is inactive");
        }

        MedicalCertificate latest = certificateService.findLatest(memberId);

        if (latest == null) {
            deny("No medical certificate registered");
        }

        if (!certificateService.isValid(latest)) {
            String reason = certificateService.getValidity(latest) == CertificateValidity.EXPIRED
                    ? "Medical certificate has expired"
                    : "Medical certificate is not valid yet";

            deny(reason);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        List<MemberSubscription> subscriptions = subscriptionRepository.findActiveForCheckIn(
                memberId,
                SubscriptionStatus.ACTIVE);

        boolean hasCurrentSubscription = false;
        boolean hasService = false;
        boolean hasPaidSubscription = false;
        boolean hasValidTimeBand = false;
        boolean hasRemainingEntries = false;

        MemberSubscription eligibleSubscription = null;

        for (MemberSubscription subscription : subscriptions) {

            if (subscription.getStartDate().isAfter(today)
                    || subscription.getEndDate().isBefore(today)) {
                continue;
            }

            hasCurrentSubscription = true;

            MembershipTier tier = subscription.getTier();

            if (!tier.getAllowedServices().contains(serviceAccessed)) {
                continue;
            }

            hasService = true;

            boolean paid = paymentRepository
                    .existsBySubscription_IdAndStatus(
                            subscription.getId(),
                            PaymentStatus.PAID);

            if (!paid) {
                continue;
            }

            hasPaidSubscription = true;

            if (!isWithinTimeBand(tier, now.toLocalTime())) {
                continue;
            }

            hasValidTimeBand = true;

            Integer remaining = subscription.getRemainingEntries();

            if (tier.getEntryQuota() != null
                    && (remaining == null || remaining <= 0)) {
                continue;
            }

            hasRemainingEntries = true;
            eligibleSubscription = subscription;
            break;
        }

        if (eligibleSubscription == null) {

            if (!hasCurrentSubscription) {
                deny("No active subscription");
            }

            if (!hasService) {
                deny("Subscription does not include this service");
            }

            if (!hasPaidSubscription) {
                deny("Subscription payment is not paid");
            }

            if (!hasValidTimeBand) {
                deny("Access outside the allowed time band");
            }

            if (!hasRemainingEntries) {
                deny("No remaining entries");
            }

            deny("Access not allowed");
        }

        if (eligibleSubscription.getTier().getEntryQuota() != null) {
            Integer remaining = eligibleSubscription.getRemainingEntries();

            eligibleSubscription.setRemainingEntries(remaining - 1);
            subscriptionRepository.save(eligibleSubscription);
        }

        AccessRecord record = new AccessRecord();
        record.setMember(member);
        record.setAccessDatetime(now);
        record.setServiceAccessed(serviceAccessed);

        AccessRecord saved = accessRecordRepository.save(record);

        return new CheckInResponse(
                saved.getId(),
                memberId,
                now,
                serviceAccessed.name(),
                "ALLOWED");
    }

    @Transactional(readOnly = true)
    public CheckInInfoResponse getCheckInInfo(Integer memberId) {

        Member member = memberService.findById(memberId);

        MedicalCertificate certificate = certificateService.findLatest(member.getId());

        CheckInInfoResponse.MedicalCertificateInfo certificateInfo = null;

        if (certificate != null) {
            certificateInfo = new CheckInInfoResponse.MedicalCertificateInfo(
                    certificate.getExpiryDate(),
                    certificateService.getValidity(certificate).name());
        }

        List<MemberSubscription> subscriptions = subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId);

        List<CheckInInfoResponse.SubscriptionInfo> subscriptionInfo = subscriptions.stream()
                .map(subscription -> {

                    MembershipTier tier = subscription.getTier();

                    TimeBandConfiguration timeBand = timeBandRepository
                            .findByTimeBand(tier.getTimeBand())
                            .orElse(null);

                    LocalTime startTime = timeBand != null ? timeBand.getStartTime() : null;

                    LocalTime endTime = timeBand != null ? timeBand.getEndTime() : null;

                    return new CheckInInfoResponse.SubscriptionInfo(
                            subscription.getId(),
                            tier.getName(),
                            subscription.getStatus(),
                            subscription.getStartDate(),
                            subscription.getEndDate(),
                            startTime,
                            endTime,
                            subscription.getRemainingEntries(),
                            Set.copyOf(tier.getAllowedServices()));
                })
                .toList();

        return new CheckInInfoResponse(
                certificateInfo,
                subscriptionInfo);
    }

    private boolean isWithinTimeBand(
            MembershipTier tier,
            LocalTime currentTime) {

        TimeBandConfiguration config = timeBandRepository
                .findByTimeBand(tier.getTimeBand())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Time band configuration not found"));

        LocalTime start = config.getStartTime();
        LocalTime end = config.getEndTime();

        if (start.equals(end)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid time band configuration");
        }

        if (start.isBefore(end)) {
            return !currentTime.isBefore(start)
                    && currentTime.isBefore(end);
        }

        return !currentTime.isBefore(start)
                || currentTime.isBefore(end);
    }

    private void deny(String reason) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied: " + reason);
    }
}