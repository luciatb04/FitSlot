package org.springframework.samples.fitslot.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberRequest;
import org.springframework.samples.fitslot.member.MemberService;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificate;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificateRepository;
import org.springframework.samples.fitslot.membership.MemberSubscription;
import org.springframework.samples.fitslot.membership.MemberSubscriptionRepository;
import org.springframework.samples.fitslot.membership.MembershipTier;
import org.springframework.samples.fitslot.membership.MembershipTierRepository;
import org.springframework.samples.fitslot.membership.ServiceType;
import org.springframework.samples.fitslot.membership.SubscriptionStatus;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.samples.fitslot.payment.PaymentRecord;
import org.springframework.samples.fitslot.payment.PaymentRecordRepository;
import org.springframework.samples.fitslot.payment.PaymentStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AccessCheckInControllerTests {

    private static final String CHECK_IN_URL = "/api/v1/access/check-in";
    private static final String VALIDATE_ACCESS_URL = "/api/v1/access/validate";
    private static final String CHECK_IN_INFO_URL = "/api/v1/access/members/%d/check-in-info";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MedicalCertificateRepository certificateRepository;

    @Autowired
    private AccessRecordRepository accessRecordRepository;

    @Autowired
    private MemberSubscriptionRepository subscriptionRepository;

    @Autowired
    private MembershipTierRepository tierRepository;

    @Autowired
    private PaymentRecordRepository paymentRepository;

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void allowsCheckInWithLatestCertificateValidThroughItsExpiryDate() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(30), today);
        createPaidSubscription(member, today.minusDays(1), today.plusDays(28), ServiceType.WEIGHT_ROOM);

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(member.getId()))
            .andExpect(jsonPath("$.result").value("ALLOWED"));

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .hasSize(1);
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void deniesCheckInWithoutCertificateAndDoesNotCreateAccessRecord() throws Exception {
        Member member = createMember();

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied: No medical certificate registered"));

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .isEmpty();
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void deniesCheckInWhenLatestCertificateIsExpiredEvenIfOlderOneWouldRemainValid() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(60), today.plusDays(30));
        createCertificate(member, today.minusDays(10), today.minusDays(1));

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied: Medical certificate has expired"));

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .isEmpty();
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void reportsValidMedicalCertificateStatusAndExpiryDate() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = today.plusDays(20);
        createCertificate(member, today.minusDays(10), expiryDate);

        mockMvc.perform(get(CHECK_IN_INFO_URL.formatted(member.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medicalCertificate.validity").value("VALID"))
            .andExpect(jsonPath("$.medicalCertificate.expiryDate").value(expiryDate.toString()))
            .andExpect(jsonPath("$.medicalCertificate.denialReason").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void reportsExpiredMedicalCertificateWithItsDenialReason() throws Exception {
        Member member = createMember();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        createCertificate(member, yesterday.minusDays(20), yesterday);

        mockMvc.perform(get(CHECK_IN_INFO_URL.formatted(member.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medicalCertificate.validity").value("EXPIRED"))
            .andExpect(jsonPath("$.medicalCertificate.denialReason")
                    .value("Medical certificate has expired"));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void reportsMissingMedicalCertificateWithItsDenialReason() throws Exception {
        Member member = createMember();

        mockMvc.perform(get(CHECK_IN_INFO_URL.formatted(member.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medicalCertificate.validity").value("MISSING"))
            .andExpect(jsonPath("$.medicalCertificate.expiryDate").doesNotExist())
            .andExpect(jsonPath("$.medicalCertificate.denialReason")
                    .value("No medical certificate registered"));
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void deniesCheckInFromMembers() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(2), today.plusDays(2));

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void deniesCheckInWhenMemberHasNoPaidActiveSubscription() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(2), today.plusDays(2));

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied: No active subscription"));

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .isEmpty();
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void deniesCheckInWithAnExplicitExpiredSubscriptionReason() throws Exception {
        Member member = createMember();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        createCertificate(member, yesterday.minusDays(30), yesterday.plusDays(30));
        createPaidSubscription(
                member, yesterday.minusDays(30), yesterday, ServiceType.WEIGHT_ROOM);

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                    .value("Access denied: Subscription expired on " + yesterday));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void deniesCheckInWithAnExplicitFrozenSubscriptionReason() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(30), today.plusDays(30));
        MemberSubscription subscription = createPaidSubscription(
                member, today.minusDays(1), today.plusDays(28), ServiceType.WEIGHT_ROOM);
        subscription.setStatus(SubscriptionStatus.FROZEN);
        subscriptionRepository.save(subscription);

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied: Subscription is frozen"));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void reportsAnActivePaidSubscriptionWithItsExpiryDate() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        MemberSubscription subscription = createPaidSubscription(
                member, today.minusDays(1), today.plusDays(28), ServiceType.WEIGHT_ROOM);

        mockMvc.perform(get(CHECK_IN_INFO_URL.formatted(member.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checkedAt").isNotEmpty())
            .andExpect(jsonPath("$.subscriptions[0].id").value(subscription.getId()))
            .andExpect(jsonPath("$.subscriptions[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.subscriptions[0].verificationStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.subscriptions[0].denialReason").doesNotExist())
            .andExpect(jsonPath("$.subscriptions[0].endDate").value(today.plusDays(28).toString()))
            .andExpect(jsonPath("$.subscriptions[0].startTime").isNotEmpty())
            .andExpect(jsonPath("$.subscriptions[0].endTime").isNotEmpty())
            .andExpect(jsonPath("$.subscriptions[0].timeSlotAllowed").exists());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void reportsAnActiveSubscriptionAsExpiredWhenItsEndDateHasPassed() throws Exception {
        Member member = createMember();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        createPaidSubscription(
                member, yesterday.minusDays(30), yesterday, ServiceType.WEIGHT_ROOM);

        mockMvc.perform(get(CHECK_IN_INFO_URL.formatted(member.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subscriptions[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.subscriptions[0].verificationStatus").value("EXPIRED"))
            .andExpect(jsonPath("$.subscriptions[0].denialReason")
                    .value("Subscription expired on " + yesterday));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void reportsFrozenSubscriptionsAsDenied() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        MemberSubscription subscription = createPaidSubscription(
                member, today.minusDays(1), today.plusDays(28), ServiceType.WEIGHT_ROOM);
        subscription.setStatus(SubscriptionStatus.FROZEN);
        subscriptionRepository.save(subscription);

        mockMvc.perform(get(CHECK_IN_INFO_URL.formatted(member.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subscriptions[0].verificationStatus").value("FROZEN"))
            .andExpect(jsonPath("$.subscriptions[0].denialReason")
                    .value("Subscription is frozen"));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void validatesAnIncludedServiceWithoutRegisteringAnEntrance() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(10), today.plusDays(20));
        createPaidSubscription(
                member, today.minusDays(1), today.plusDays(28), ServiceType.WEIGHT_ROOM);

        mockMvc.perform(post(VALIDATE_ACCESS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorized").value(true))
            .andExpect(jsonPath("$.reason").doesNotExist());

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .isEmpty();
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void returnsAReasonWhenTheRequestedServiceIsNotIncluded() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(10), today.plusDays(20));
        createPaidSubscription(
                member, today.minusDays(1), today.plusDays(28), ServiceType.POOL);

        mockMvc.perform(post(VALIDATE_ACCESS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorized").value(false))
            .andExpect(jsonPath("$.reason").value(
                    "Subscription does not include this service"));

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .isEmpty();
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void validatesEntryBasedMembershipWithoutConsumingAnEntryUntilCheckIn() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(10), today.plusDays(20));
        MemberSubscription subscription = createPaidEntrySubscription(
                member, today.minusDays(1), today.plusDays(28), ServiceType.WEIGHT_ROOM, 3);

        mockMvc.perform(post(VALIDATE_ACCESS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorized").value(true));

        assertThat(subscriptionRepository.findById(subscription.getId()).orElseThrow()
                .getRemainingEntries()).isEqualTo(3);

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("ALLOWED"));

        assertThat(subscriptionRepository.findById(subscription.getId()).orElseThrow()
                .getRemainingEntries()).isEqualTo(2);
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void deniesCheckInForAServiceNotIncludedInThePaidSubscription() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(2), today.plusDays(2));
        createPaidSubscription(member, today.minusDays(1), today.plusDays(28), ServiceType.POOL);

        mockMvc.perform(post(CHECK_IN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(member)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(
                "Access denied: Subscription does not include this service"));
    }

    private Member createMember() {
        String email = "checkin-" + UUID.randomUUID() + "@example.com";
        return memberService.create(new MemberRequest("Ada", "Lovelace", email, null));
    }

    private void createCertificate(Member member, LocalDate issueDate, LocalDate expiryDate) {
        MedicalCertificate certificate = new MedicalCertificate();
        certificate.setMember(member);
        certificate.setIssueDate(issueDate);
        certificate.setExpiryDate(expiryDate);
        certificateRepository.save(certificate);
    }

    private MemberSubscription createPaidSubscription(
            Member member, LocalDate startDate, LocalDate endDate, ServiceType service) {
        MembershipTier tier = new MembershipTier();
        tier.setName("Check-in tier " + UUID.randomUUID());
        tier.setPrice(new BigDecimal("25.00"));
        tier.setDurationDays(30);
        tier.setTimeBand(TimeBand.FULL_DAY);
        tier.setAllowedServices(java.util.Set.of(service));
        MembershipTier savedTier = tierRepository.save(tier);

        MemberSubscription subscription = new MemberSubscription();
        subscription.setMember(member);
        subscription.setTier(savedTier);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setFrozenDaysUsed(0);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        MemberSubscription savedSubscription = subscriptionRepository.save(subscription);

        PaymentRecord payment = new PaymentRecord();
        payment.setMember(member);
        payment.setSubscription(savedSubscription);
        payment.setAmount(savedTier.getPrice());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        return savedSubscription;
    }

    private MemberSubscription createPaidEntrySubscription(
            Member member,
            LocalDate startDate,
            LocalDate endDate,
            ServiceType service,
            int entryQuota) {
        MembershipTier tier = new MembershipTier();
        tier.setName("Entry tier " + UUID.randomUUID());
        tier.setPrice(new BigDecimal("25.00"));
        tier.setEntryQuota(entryQuota);
        tier.setTimeBand(TimeBand.FULL_DAY);
        tier.setAllowedServices(java.util.Set.of(service));
        MembershipTier savedTier = tierRepository.save(tier);

        MemberSubscription subscription = new MemberSubscription();
        subscription.setMember(member);
        subscription.setTier(savedTier);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setRemainingEntries(entryQuota);
        subscription.setFrozenDaysUsed(0);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        MemberSubscription savedSubscription = subscriptionRepository.save(subscription);

        PaymentRecord payment = new PaymentRecord();
        payment.setMember(member);
        payment.setSubscription(savedSubscription);
        payment.setAmount(savedTier.getPrice());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        return savedSubscription;
    }

    private String requestJson(Member member) throws Exception {
        return objectMapper.writeValueAsString(new CheckInRequest(member.getId(), ServiceType.WEIGHT_ROOM));
    }
}
