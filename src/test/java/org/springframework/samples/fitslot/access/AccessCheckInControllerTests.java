package org.springframework.samples.fitslot.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificate;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificateRepository;
import org.springframework.samples.fitslot.membership.ServiceType;
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

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void allowsCheckInWithLatestCertificateValidThroughItsExpiryDate() throws Exception {
        Member member = createMember();
        LocalDate today = LocalDate.now();
        createCertificate(member, today.minusDays(30), today);

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

    private String requestJson(Member member) throws Exception {
        return objectMapper.writeValueAsString(new CheckInRequest(member.getId(), ServiceType.WEIGHT_ROOM));
    }
}
