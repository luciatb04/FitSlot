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
    private TimeBandConfigurationRepository timeBandRepository = null;
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
        LocalDateTime now = LocalDateTime.now(clock);
        AccessDecision decision = evaluateAccess(memberId, serviceAccessed, now, true);

        if (!decision.authorized()) {
            deny(decision.denialReason());
        }

        MemberSubscription eligibleSubscription = decision.subscription();

        if (eligibleSubscription.getTier().getEntryQuota() != null) {
            Integer remaining = eligibleSubscription.getRemainingEntries();

            eligibleSubscription.setRemainingEntries(remaining - 1);
            subscriptionRepository.save(eligibleSubscription);
        }

        AccessRecord record = new AccessRecord();
        record.setMember(decision.member());
        record.setAccessDatetime(decision.checkedAt());
        record.setServiceAccessed(serviceAccessed);

        AccessRecord saved = accessRecordRepository.save(record);

        return new CheckInResponse(
                saved.getId(),
                memberId,
                decision.checkedAt(),
                serviceAccessed.name(),
                "ALLOWED");
    }

    @Transactional(readOnly = true)
    public AccessValidationResponse validateAccess(
            Integer memberId,
            ServiceType serviceAccessed) {
        AccessDecision decision = evaluateAccess(
                memberId, serviceAccessed, LocalDateTime.now(clock), false);
        return new AccessValidationResponse(
                decision.authorized(),
                decision.denialReason());
    }

    private AccessDecision evaluateAccess(
            Integer memberId,
            ServiceType serviceAccessed,
            LocalDateTime now,
            boolean lockSubscriptions) {
        Member member = memberService.findById(memberId);

        if (member.getStatus() != MemberStatus.ACTIVE) {
            return AccessDecision.denied(member, now, "Member is inactive");
        }

        MedicalCertificate latest = certificateService.findLatest(memberId);
        if (latest == null) {
            return AccessDecision.denied(member, now, "No medical certificate registered");
        }

        if (!certificateService.isValid(latest)) {
            String reason = certificateService.getValidity(latest) == CertificateValidity.EXPIRED
                    ? "Medical certificate has expired"
                    : "Medical certificate is not valid yet";
            return AccessDecision.denied(member, now, reason);
        }

        LocalDate today = now.toLocalDate();
        List<MemberSubscription> subscriptions = lockSubscriptions
                ? subscriptionRepository.findActiveForCheckIn(
                        memberId, SubscriptionStatus.ACTIVE)
                : subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId);

        boolean hasCurrentSubscription = false;
        boolean hasService = false;
        boolean hasPaidSubscription = false;
        boolean hasValidTimeBand = false;
        boolean hasRemainingEntries = false;
        MemberSubscription eligibleSubscription = null;

        for (MemberSubscription subscription : subscriptions) {
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                    || subscription.getStartDate().isAfter(today)
                    || subscription.getEndDate().isBefore(today)) {
                continue;
            }

            hasCurrentSubscription = true;
            MembershipTier tier = subscription.getTier();
            if (!tier.getAllowedServices().contains(serviceAccessed)) {
                continue;
            }

            hasService = true;
            boolean paid = paymentRepository.existsBySubscription_IdAndStatus(
                    subscription.getId(), PaymentStatus.PAID);
            if (!paid) {
                continue;
            }

            hasPaidSubscription = true;
            if (!isWithinTimeBand(findTimeBandConfiguration(tier), now.toLocalTime())) {
                continue;
            }

            hasValidTimeBand = true;
            Integer remaining = subscription.getRemainingEntries();
            if (tier.getEntryQuota() != null && (remaining == null || remaining <= 0)) {
                continue;
            }

            hasRemainingEntries = true;
            eligibleSubscription = subscription;
            break;
        }

        if (eligibleSubscription != null) {
            return new AccessDecision(member, eligibleSubscription, now, null);
        }

        String denialReason;
        if (!hasCurrentSubscription) {
            denialReason = subscriptionDenialReason(memberId, today);
        } else if (!hasService) {
            denialReason = "Subscription does not include this service";
        } else if (!hasPaidSubscription) {
            denialReason = "Subscription payment is not paid";
        } else if (!hasValidTimeBand) {
            denialReason = "Access outside the allowed time band";
        } else if (!hasRemainingEntries) {
            denialReason = "No remaining entries";
        } else {
            denialReason = "Access not allowed";
        }

        return AccessDecision.denied(member, now, denialReason);
    }

    @Transactional(readOnly = true)
    public CheckInInfoResponse getCheckInInfo(Integer memberId) {

        Member member = memberService.findById(memberId);

        MedicalCertificate certificate = certificateService.findLatest(member.getId());

        CheckInInfoResponse.MedicalCertificateInfo certificateInfo;

        if (certificate == null) {
            certificateInfo = new CheckInInfoResponse.MedicalCertificateInfo(
                    null,
                    "MISSING",
                    "No medical certificate registered");
        } else {
            CertificateValidity validity = certificateService.getValidity(certificate);
            String denialReason = switch (validity) {
                case VALID -> null;
                case EXPIRED -> "Medical certificate has expired";
                case NOT_YET_VALID -> "Medical certificate is not valid yet";
            };
            certificateInfo = new CheckInInfoResponse.MedicalCertificateInfo(
                    certificate.getExpiryDate(),
                    validity.name(),
                    denialReason);
        }

        List<MemberSubscription> subscriptions = subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId);

        LocalDateTime checkedAt = LocalDateTime.now(clock);
        LocalDate today = checkedAt.toLocalDate();
        LocalTime currentTime = checkedAt.toLocalTime();
        List<CheckInInfoResponse.SubscriptionInfo> subscriptionInfo = subscriptions.stream()
                .map(subscription -> {

                    MembershipTier tier = subscription.getTier();
                    SubscriptionVerification verification =
                            verifySubscription(subscription, today);

                    TimeBandConfiguration timeBand = findTimeBandConfiguration(tier);
                    boolean timeSlotAllowed =
                            isWithinTimeBand(timeBand, currentTime);
                    String timeSlotDenialReason = timeSlotAllowed
                            ? null
                            : "Access outside the allowed time band";

                    return new CheckInInfoResponse.SubscriptionInfo(
                            subscription.getId(),
                            tier.getName(),
                            subscription.getStatus(),
                            verification.status(),
                            verification.denialReason(),
                            subscription.getStartDate(),
                            subscription.getEndDate(),
                            timeBand.getStartTime(),
                            timeBand.getEndTime(),
                            timeSlotAllowed,
                            timeSlotDenialReason,
                            subscription.getRemainingEntries(),
                            Set.copyOf(tier.getAllowedServices()));
                })
                .toList();

        return new CheckInInfoResponse(
                checkedAt,
                certificateInfo,
                subscriptionInfo);
    }

    private SubscriptionVerification verifySubscription(
            MemberSubscription subscription,
            LocalDate today) {
        if (subscription.getStatus() == SubscriptionStatus.FROZEN) {
            return new SubscriptionVerification(
                    SubscriptionStatus.FROZEN,
                    "Subscription is frozen");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            return new SubscriptionVerification(
                    SubscriptionStatus.CANCELLED,
                    "Subscription is cancelled");
        }

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            return new SubscriptionVerification(
                    SubscriptionStatus.EXPIRED,
                    "Subscription is marked as expired");
        }

        if (subscription.getEndDate().isBefore(today)) {
            return new SubscriptionVerification(
                    SubscriptionStatus.EXPIRED,
                    "Subscription expired on " + subscription.getEndDate());
        }

        if (subscription.getStatus() == SubscriptionStatus.PENDING
                || subscription.getStartDate().isAfter(today)) {
            return new SubscriptionVerification(
                    SubscriptionStatus.PENDING,
                    "Subscription is not active yet");
        }

        if (!paymentRepository.existsBySubscription_IdAndStatus(
                subscription.getId(), PaymentStatus.PAID)) {
            return new SubscriptionVerification(
                    SubscriptionStatus.PENDING,
                    "Subscription payment is not paid");
        }

        return new SubscriptionVerification(SubscriptionStatus.ACTIVE, null);
    }

    private String subscriptionDenialReason(Integer memberId, LocalDate today) {
        List<MemberSubscription> subscriptions =
                subscriptionRepository.findByMember_IdOrderByStartDateDesc(memberId);

        for (MemberSubscription subscription : subscriptions) {
            if (subscription.getStatus() == SubscriptionStatus.FROZEN) {
                return "Subscription is frozen";
            }
            if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
                return "Subscription is cancelled";
            }
            if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
                return "Subscription is marked as expired";
            }
            if (subscription.getStatus() == SubscriptionStatus.PENDING
                    || subscription.getStartDate().isAfter(today)) {
                return "Subscription is not active yet";
            }
            if (subscription.getEndDate().isBefore(today)) {
                return "Subscription expired on " + subscription.getEndDate();
            }
        }

        return "No active subscription";
    }

    private record SubscriptionVerification(
            SubscriptionStatus status,
            String denialReason) {
    }

    private record AccessDecision(
            Member member,
            MemberSubscription subscription,
            LocalDateTime checkedAt,
            String denialReason) {

        private boolean authorized() {
            return subscription != null;
        }

        private static AccessDecision denied(
                Member member,
                LocalDateTime checkedAt,
                String reason) {
            return new AccessDecision(member, null, checkedAt, reason);
        }
    }

    private TimeBandConfiguration findTimeBandConfiguration(MembershipTier tier) {
        return timeBandRepository
                .findByTimeBand(tier.getTimeBand())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Time band configuration not found"));
    }

    private boolean isWithinTimeBand(
            TimeBandConfiguration config,
            LocalTime currentTime) {

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