package org.springframework.samples.fitslot.medicalcertificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MedicalCertificateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MedicalCertificateRepository certificateRepository;

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void createsAndListsCertificateHistoryNewestFirst() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        MedicalCertificateRequest older = new MedicalCertificateRequest(today.minusDays(20), today.plusDays(20));
        MvcResult olderResult = mockMvc.perform(post(baseUrl(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(older)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.memberId").value(member.getId()))
            .andExpect(jsonPath("$.validity").value("VALID"))
            .andReturn();

        MedicalCertificateRequest latest = new MedicalCertificateRequest(today.minusDays(1), today.plusDays(10));
        mockMvc.perform(post(baseUrl(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(latest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.validity").value("VALID"));

        mockMvc.perform(get(baseUrl(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].issueDate").value(today.minusDays(1).toString()))
            .andExpect(jsonPath("$[1].issueDate").value(today.minusDays(20).toString()));

        Integer olderId = objectMapper.readTree(olderResult.getResponse().getContentAsString())
            .get("id").asInt();
        assertThat(certificateRepository.findById(olderId)).isPresent();
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void rejectsFutureIssueDateAndExpiryBeforeIssueDate() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();

        mockMvc.perform(post(baseUrl(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MedicalCertificateRequest(today.plusDays(1), today.plusDays(30)))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Issue date cannot be in the future"));

        mockMvc.perform(post(baseUrl(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MedicalCertificateRequest(today, today.minusDays(1)))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Expiry date must be on or after issue date"));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void marksCertificateExpiredWhenExpiryDateIsBeforeToday() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();

        mockMvc.perform(post(baseUrl(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MedicalCertificateRequest(today.minusDays(20), today.minusDays(1)))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.validity").value("EXPIRED"));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void updatesExistingCertificateDatesWithoutCreatingAnotherRecord() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        MvcResult createdResult = mockMvc.perform(post(baseUrl(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MedicalCertificateRequest(today.minusDays(10), today.plusDays(10)))))
            .andExpect(status().isCreated())
            .andReturn();
        int certificateId = objectMapper.readTree(createdResult.getResponse().getContentAsString())
            .get("id").asInt();

        mockMvc.perform(put(baseUrl(member) + "/" + certificateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new MedicalCertificateRequest(today.minusDays(8), today.plusDays(20)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(certificateId))
            .andExpect(jsonPath("$.expiryDate").value(today.plusDays(20).toString()));

        assertThat(certificateRepository.findByMember_IdOrderByIssueDateDescIdDesc(member.getId()))
            .hasSize(1);
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void deniesCertificateManagementToMembers() throws Exception {
        Member member = createMember();
        mockMvc.perform(get(baseUrl(member)))
            .andExpect(status().isForbidden());
    }

    private Member createMember() {
        String email = "certificate-" + UUID.randomUUID() + "@example.com";
        return memberService.create(new MemberRequest("Ada", "Lovelace", email, null));
    }

    private String baseUrl(Member member) {
        return "/api/v1/members/" + member.getId() + "/medical-certificates";
    }
}
