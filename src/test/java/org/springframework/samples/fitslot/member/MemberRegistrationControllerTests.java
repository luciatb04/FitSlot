package org.springframework.samples.fitslot.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MemberRegistrationControllerTests {

    private static final String BASE_URL = "/api/v1/members";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void deniesUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void seededReceptionistCanSignInAndRegisterMembers() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"receptionist1","password":"Receptionist123!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("receptionist1"))
            .andExpect(jsonPath("$.roles[0]").value("RECEPTIONIST"))
            .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .get("token").asText();
        MemberRequest request = new MemberRequest("Ada", "Lovelace", uniqueEmail(), null);

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void deniesRequestsFromMembers() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void registersAndPersistsAnActiveMember() throws Exception {
        String email = uniqueEmail();
        MemberRequest request = new MemberRequest("Ada", "Lovelace", email, "123456789");

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.firstName").value("Ada"))
            .andExpect(jsonPath("$.lastName").value("Lovelace"))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.telephone").value("123456789"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        Member persisted = memberRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(persisted.getFirstName()).isEqualTo("Ada");
        assertThat(persisted.getLastName()).isEqualTo("Lovelace");
        assertThat(persisted.getTelephone()).isEqualTo("123456789");
        assertThat(persisted.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void allowsAdminToRegisterAMember() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void rejectsMissingOrInvalidRequiredFields() throws Exception {
        MemberRequest request = new MemberRequest(" ", "Lovelace", "not-an-email", null);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        MemberRequest original = new MemberRequest("Ada", "Lovelace", email, null);
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(original)))
            .andExpect(status().isCreated());

        MemberRequest duplicate = new MemberRequest("Augusta", "King", email.toUpperCase(), null);
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    private String validRequestJson() throws Exception {
        return objectMapper.writeValueAsString(
            new MemberRequest("Ada", "Lovelace", uniqueEmail(), null)
        );
    }

    private String uniqueEmail() {
        return "member-" + UUID.randomUUID() + "@example.com";
    }
}
