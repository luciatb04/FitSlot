package org.springframework.samples.fitslot.access;

import static org.assertj.core.api.Assertions.assertThat;
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
            .andExpect(jsonPath("$.message").value("Access denied: no medical certificate registered"));

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
            .andExpect(jsonPath("$.message").value("Access denied: latest medical certificate has expired"));

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .isEmpty();
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
            .andExpect(jsonPath("$.message").value("Access denied: no active subscription"));

        assertThat(accessRecordRepository.findByMember_IdOrderByAccessDatetimeDesc(member.getId()))
            .isEmpty();
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
                "Access denied: active subscription does not include this service"));
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

    private void createPaidSubscription(
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
    }

    private String requestJson(Member member) throws Exception {
        return objectMapper.writeValueAsString(new CheckInRequest(member.getId(), ServiceType.WEIGHT_ROOM));
    }
}
