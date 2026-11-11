
package org.springframework.samples.fitslot.member;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private Member createTestMember() {
        String email = "member-" + UUID.randomUUID() + "@example.com";

        return memberService.create(
            new MemberRequest(
                "John",
                "Smith",
                email,
                "123456789"
            )
        );
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCanCreateMember() throws Exception {
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Ada",
                      "lastName": "Lovelace",
                      "email": "ada-%s@example.com",
                      "telephone": "123456789"
                    }
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("Ada"))
            .andExpect(jsonPath("$.lastName").value("Lovelace"));
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void nonReceptionistCannotCreateMember() throws Exception {
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Ada",
                      "lastName": "Lovelace",
                      "email": "forbidden@example.com"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCanListMembers() throws Exception {
        Member member = createTestMember();

        mockMvc.perform(get("/api/v1/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].email",
                hasItem(member.getEmail())));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCanViewMemberDetails() throws Exception {
        Member member = createTestMember();

        mockMvc.perform(get("/api/v1/members/{id}", member.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(member.getId()))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.email").value(member.getEmail()))
            .andExpect(jsonPath("$.telephone").value("123456789"));
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCanUpdateMemberDetails() throws Exception {
        Member member = createTestMember();
        String newEmail = "updated-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(put("/api/v1/members/{id}", member.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "James",
                      "lastName": "Wilson",
                      "email": "%s",
                      "telephone": "987654321"
                    }
                    """.formatted(newEmail)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("James"))
            .andExpect(jsonPath("$.lastName").value("Wilson"))
            .andExpect(jsonPath("$.email").value(newEmail))
            .andExpect(jsonPath("$.telephone").value("987654321"));

        entityManager.flush();
        entityManager.clear();

        Member updatedMember = memberRepository
            .findById(member.getId())
            .orElseThrow();

        assertEquals("James", updatedMember.getFirstName());
        assertEquals("Wilson", updatedMember.getLastName());
        assertEquals(newEmail, updatedMember.getEmail());
        assertEquals("987654321", updatedMember.getTelephone());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCannotUpdateMemberWithInvalidEmail() throws Exception {
        Member member = createTestMember();

        mockMvc.perform(put("/api/v1/members/{id}", member.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "James",
                      "lastName": "Wilson",
                      "email": "invalid-email",
                      "telephone": "123456789"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCannotUpdateMemberWithEmptyName() throws Exception {
        Member member = createTestMember();

        mockMvc.perform(put("/api/v1/members/{id}", member.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "",
                      "lastName": "Wilson",
                      "email": "james.wilson@example.com",
                      "telephone": "123456789"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCannotUpdateMemberWithDuplicateEmail() throws Exception {
        Member firstMember = createTestMember();
        Member secondMember = createTestMember();

        mockMvc.perform(put("/api/v1/members/{id}", secondMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "James",
                      "lastName": "Wilson",
                      "email": "%s",
                      "telephone": "123456789"
                    }
                    """.formatted(firstMember.getEmail())))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCannotUpdateNonExistingMember() throws Exception {
        mockMvc.perform(put("/api/v1/members/{id}", Integer.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "James",
                      "lastName": "Wilson",
                      "email": "james.wilson@example.com",
                      "telephone": "123456789"
                    }
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void nonReceptionistCannotUpdateMember() throws Exception {
        mockMvc.perform(put("/api/v1/members/{id}", Integer.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "James",
                      "lastName": "Wilson",
                      "email": "james.wilson@example.com",
                      "telephone": "123456789"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void receptionistCannotViewNonExistingMember() throws Exception {
        mockMvc.perform(get("/api/v1/members/{id}", Integer.MAX_VALUE))
            .andExpect(status().isNotFound());
    }
}
