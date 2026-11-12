package org.springframework.samples.fitslot.membership;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MembershipTierSecurityTests {

    private static final String BASE_URL = "/api/membership-tiers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void allowsReceptionistsToReadMembershipTiers() throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void allowsGymOwnerToCreateMembershipTier() throws Exception {
        MembershipTierRequest request = new MembershipTierRequest(
                "Integration " + UUID.randomUUID(),
                new BigDecimal("19.99"),
                null,
                5,
                Set.of(ServiceType.POOL),
                TimeBand.FULL_DAY);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.durationDays").doesNotExist())
                .andExpect(jsonPath("$.entryQuota").value(5));
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void deniesMembershipCreationFromNonGymOwner() throws Exception {
        MembershipTierRequest request = new MembershipTierRequest(
                "Forbidden " + UUID.randomUUID(),
                new BigDecimal("19.99"),
                30,
                null,
                Set.of(ServiceType.POOL),
                TimeBand.FULL_DAY);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RECEPTIONIST")
    void deniesMembershipCreationFromReceptionist() throws Exception {
        MembershipTierRequest request = new MembershipTierRequest(
                "Forbidden " + UUID.randomUUID(),
                new BigDecimal("19.99"),
                30,
                null,
                Set.of(ServiceType.POOL),
                TimeBand.FULL_DAY);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
