package org.springframework.samples.fitslot.membership;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TimeBandConfigurationControllerTests {

    private static final String BASE_URL = "/api/time-bands";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deniesUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    void deniesRequestsFromNonGymOwners() throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void returnsConfiguredTimeBands() throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.timeBand == 'FULL_DAY')]").exists())
            .andExpect(jsonPath("$[?(@.timeBand == 'MORNINGS_ONLY')]").exists())
            .andExpect(jsonPath("$[?(@.timeBand == 'AFTERNOONS_ONLY')]").exists())
            .andExpect(jsonPath("$[?(@.timeBand == 'EVENINGS_ONLY')]").exists());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void updatesTimeBandAndPersistsNewHours() throws Exception {
        String request = """
            {
              "startTime": "06:30:00",
              "endTime": "11:45:00"
            }
            """;

        mockMvc.perform(put(BASE_URL + "/MORNINGS_ONLY")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timeBand").value("MORNINGS_ONLY"))
            .andExpect(jsonPath("$.startTime").value("06:30:00"))
            .andExpect(jsonPath("$.endTime").value("11:45:00"));

        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.timeBand == 'MORNINGS_ONLY')].startTime")
                .value(org.hamcrest.Matchers.hasItem("06:30:00")))
            .andExpect(jsonPath("$[?(@.timeBand == 'MORNINGS_ONLY')].endTime")
                .value(org.hamcrest.Matchers.hasItem("11:45:00")));
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void rejectsEndTimeBeforeStartTime() throws Exception {
        String request = """
            {
              "startTime": "18:00:00",
              "endTime": "08:00:00"
            }
            """;

        mockMvc.perform(put(BASE_URL + "/FULL_DAY")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void rejectsEqualStartAndEndTime() throws Exception {
        String request = """
            {
              "startTime": "10:00:00",
              "endTime": "10:00:00"
            }
            """;

        mockMvc.perform(put(BASE_URL + "/FULL_DAY")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void rejectsMissingTime() throws Exception {
        String request = """
            {
              "startTime": "10:00:00"
            }
            """;

        mockMvc.perform(put(BASE_URL + "/FULL_DAY")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void rejectsMalformedTime() throws Exception {
        String request = """
            {
              "startTime": "not-a-time",
              "endTime": "18:00:00"
            }
            """;

        mockMvc.perform(put(BASE_URL + "/FULL_DAY")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "GYM_OWNER")
    void rejectsUnknownTimeBand() throws Exception {
        String request = """
            {
              "startTime": "08:00:00",
              "endTime": "18:00:00"
            }
            """;

        mockMvc.perform(put(BASE_URL + "/UNKNOWN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }
}
