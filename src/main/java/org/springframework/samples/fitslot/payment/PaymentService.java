package org.springframework.samples.fitslot.payment;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.exceptions.ResourceNotFoundException;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberService;
import org.springframework.samples.fitslot.membership.MemberSubscription;
import org.springframework.samples.fitslot.membership.MemberSubscriptionRepository;
import org.springframework.samples.fitslot.membership.MembershipTier;
import org.springframework.samples.fitslot.membership.MembershipTierRepository;
import org.springframework.samples.fitslot.membership.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final PaymentRecordRepository paymentRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final MembershipTierRepository tierRepository;
    private final MemberService memberService;
    private final Clock clock;

    public PaymentService(
            PaymentRecordRepository paymentRepository,
            MemberSubscriptionRepository subscriptionRepository,
            MembershipTierRepository tierRepository,
            MemberService memberService,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tierRepository = tierRepository;
        this.memberService = memberService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PaymentRecordResponse> findAll(Integer memberId) {
        List<PaymentRecord> records = memberId == null
            ? toList(paymentRepository.findAll())
            : paymentRepository.findByMember_IdOrderByPaymentDateDescIdDesc(memberId);
        if (memberId != null) {
            memberService.findById(memberId);
        }
        return records.stream()
            .sorted(Comparator.comparing(PaymentRecord::getPaymentDate).reversed()
                .thenComparing(PaymentRecord::getId, Comparator.reverseOrder()))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public PaymentRecordResponse create(PaymentRequest request) {
        Member member = memberService.findById(request.memberId());
        MembershipTier tier = tierRepository.findById(request.tierId())
            .orElseThrow(() -> new ResourceNotFoundException("Membership tier", "ID", request.tierId()));

        validateInitialStatus(request.status());
        LocalDate endDate = calculateEndDate(tier, request.startDate(), request.endDate());

        MemberSubscription subscription = new MemberSubscription();
        subscription.setMember(member);
        subscription.setTier(tier);
        subscription.setStartDate(request.startDate());
        subscription.setEndDate(endDate);
        subscription.setRemainingEntries(tier.getEntryQuota());
        subscription.setFrozenDaysUsed(0);
        subscription.setStatus(request.status() == PaymentStatus.PAID
            ? SubscriptionStatus.ACTIVE
            : SubscriptionStatus.PENDING);
        MemberSubscription savedSubscription = subscriptionRepository.save(subscription);

        PaymentRecord payment = new PaymentRecord();
        payment.setMember(member);
        payment.setSubscription(savedSubscription);
        payment.setAmount(tier.getPrice());
        payment.setPaymentDate(LocalDateTime.now(clock));
        payment.setStatus(request.status());
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentRecordResponse updateStatus(Integer paymentId, PaymentStatus newStatus) {
        PaymentRecord payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "ID", paymentId));
        PaymentStatus currentStatus = payment.getStatus();

        if (currentStatus == newStatus) {
            return toResponse(payment);
        }
        if (currentStatus == PaymentStatus.PENDING && newStatus == PaymentStatus.PAID) {
            payment.getSubscription().setStatus(SubscriptionStatus.ACTIVE);
        } else if (currentStatus == PaymentStatus.PAID && newStatus == PaymentStatus.REFUNDED) {
            payment.getSubscription().setStatus(SubscriptionStatus.CANCELLED);
        } else {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payment status can only change from PENDING to PAID or from PAID to REFUNDED"
            );
        }

        payment.setStatus(newStatus);
        subscriptionRepository.save(payment.getSubscription());
        return toResponse(paymentRepository.save(payment));
    }

    private void validateInitialStatus(PaymentStatus status) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A payment must be created as PENDING or PAID"
            );
        }
    }

    private LocalDate calculateEndDate(MembershipTier tier, LocalDate startDate, LocalDate requestedEndDate) {
        if (startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is required");
        }

        if (tier.getDurationDays() != null) {
            if (tier.getDurationDays() <= 0 || requestedEndDate != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid duration membership dates");
            }
            return startDate.plusDays(tier.getDurationDays() - 1L);
        }

        if (tier.getEntryQuota() == null || tier.getEntryQuota() <= 0
                || requestedEndDate == null || requestedEndDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "An entry-based membership requires an end date on or after its start date"
            );
        }
        return requestedEndDate;
    }

    private PaymentRecordResponse toResponse(PaymentRecord payment) {
        MemberSubscription subscription = payment.getSubscription();
        Member member = payment.getMember();
        return new PaymentRecordResponse(
            payment.getId(),
            member.getId(),
            member.getFirstName() + " " + member.getLastName(),
            subscription.getId(),
            subscription.getTier().getId(),
            subscription.getTier().getName(),
            payment.getAmount(),
            payment.getPaymentDate(),
            payment.getStatus(),
            subscription.getStatus(),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.getRemainingEntries()
        );
    }

    private List<PaymentRecord> toList(Iterable<PaymentRecord> records) {
        List<PaymentRecord> result = new ArrayList<>();
        records.forEach(result::add);
        return result;
    }
}
