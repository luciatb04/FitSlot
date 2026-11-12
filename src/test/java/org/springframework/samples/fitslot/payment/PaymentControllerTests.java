package org.springframework.samples.fitslot.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
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
import org.springframework.samples.fitslot.membership.MemberSubscriptionRepository;
import org.springframework.samples.fitslot.membership.MembershipTier;
import org.springframework.samples.fitslot.membership.MembershipTierRepository;
import org.springframework.samples.fitslot.membership.ServiceType;
import org.springframework.samples.fitslot.membership.SubscriptionStatus;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTests {

    private static final String BASE_URL = "/api/v1/payments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MembershipTierRepository tierRepository;

    @Autowired
    private MemberSubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRecordRepository paymentRepository;

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void createsDurationSubscriptionAndPaidPaymentAtomically() throws Exception {
        Member member = createMember();
        MembershipTier tier = createTier(30, null);
        LocalDate startDate = LocalDate.now().plusDays(1);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PaymentRequest(member.getId(), tier.getId(), startDate, null, PaymentStatus.PAID))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.memberId").value(member.getId()))
            .andExpect(jsonPath("$.tierId").value(tier.getId()))
            .andExpect(jsonPath("$.amount").value(25.00))
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.startDate").value(startDate.toString()))
            .andExpect(jsonPath("$.endDate").value(startDate.plusDays(29).toString()));

        assertThat(subscriptionRepository.findByMember_IdOrderByStartDateDesc(member.getId())).hasSize(1);
        assertThat(paymentRepository.findByMember_IdOrderByPaymentDateDescIdDesc(member.getId())).hasSize(1);
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void supportsEntryPlansWithReceptionProvidedEndDateAndPendingStatus() throws Exception {
        Member member = createMember();
        MembershipTier tier = createTier(null, 10);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(2);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PaymentRequest(member.getId(), tier.getId(), startDate, endDate, PaymentStatus.PENDING))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.subscriptionStatus").value("PENDING"))
            .andExpect(jsonPath("$.remainingEntries").value(10))
            .andExpect(jsonPath("$.endDate").value(endDate.toString()));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void activatesPendingPaymentAndCancelsSubscriptionOnRefund() throws Exception {
        Member member = createMember();
        MembershipTier tier = createTier(30, null);
        LocalDate startDate = LocalDate.now();
        String createResponse = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PaymentRequest(member.getId(), tier.getId(), startDate, null, PaymentStatus.PENDING))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        int paymentId = objectMapper.readTree(createResponse).get("id").asInt();

        mockMvc.perform(put(BASE_URL + "/" + paymentId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PaymentStatusRequest(PaymentStatus.PAID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"));

        mockMvc.perform(put(BASE_URL + "/" + paymentId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PaymentStatusRequest(PaymentStatus.REFUNDED))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REFUNDED"))
            .andExpect(jsonPath("$.subscriptionStatus").value("CANCELLED"));

        assertThat(paymentRepository.findById(paymentId)).get()
            .extracting(PaymentRecord::getStatus).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(subscriptionRepository.findByMember_IdOrderByStartDateDesc(member.getId()))
            .extracting(subscription -> subscription.getStatus())
            .containsExactly(SubscriptionStatus.CANCELLED);
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void rejectsPastStartDateAndEntryPlanWithoutEndDate() throws Exception {
        Member member = createMember();
        MembershipTier durationTier = createTier(30, null);
        MembershipTier entryTier = createTier(null, 10);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PaymentRequest(
                        member.getId(), durationTier.getId(), null, null, PaymentStatus.PAID))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PaymentRequest(member.getId(), entryTier.getId(), LocalDate.now(), null, PaymentStatus.PAID))))
            .andExpect(status().isBadRequest());

        assertThat(paymentRepository.findByMember_IdOrderByPaymentDateDescIdDesc(member.getId())).isEmpty();
        assertThat(subscriptionRepository.findByMember_IdOrderByStartDateDesc(member.getId())).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void listsPaymentHistoryForSelectedMember() throws Exception {
        Member member = createMember();
        MembershipTier tier = createTier(30, null);
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new PaymentRequest(member.getId(), tier.getId(), LocalDate.now(), null, PaymentStatus.PAID))))
            .andExpect(status().isCreated());

        mockMvc.perform(get(BASE_URL).param("memberId", member.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].memberId").value(member.getId()));
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void deniesPaymentManagementToMembers() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
    }

    private Member createMember() {
        return memberService.create(new MemberRequest(
            "Ada", "Lovelace", "payment-" + UUID.randomUUID() + "@example.com", null));
    }

    private MembershipTier createTier(Integer durationDays, Integer entryQuota) {
        MembershipTier tier = new MembershipTier();
        tier.setName("Payment tier " + UUID.randomUUID());
        tier.setPrice(new BigDecimal("25.00"));
        tier.setDurationDays(durationDays);
        tier.setEntryQuota(entryQuota);
        tier.setAllowedServices(Set.of(ServiceType.WEIGHT_ROOM));
        tier.setTimeBand(TimeBand.FULL_DAY);
        return tierRepository.save(tier);
    }
}
