package org.springframework.samples.fitslot.membership;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MembershipTierSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deniesUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/membership-tiers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void deniesRequestsFromNonGymOwners() throws Exception {
        mockMvc.perform(get("/api/membership-tiers"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void allowsRequestsFromGymOwners() throws Exception {
        mockMvc.perform(get("/api/membership-tiers"))
            .andExpect(status().isOk());
    }
}
