package org.springframework.samples.fitslot.membership;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.fitslot.configuration.SecurityConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MembershipTierRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class MembershipTierRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MembershipTierService membershipTierService;

    @Test
    void createsTierFromJson() throws Exception {
        MembershipTier tier = new MembershipTier();
        tier.setId(100);
        tier.setName("Mensual completo");
        tier.setPrice(new BigDecimal("45.00"));
        tier.setDurationDays(30);
        tier.setEntryQuota(null);
        tier.setAllowedServices(Set.of(
            ServiceType.WEIGHT_ROOM,
            ServiceType.CLASSES
        ));
        tier.setTimeBand(TimeBand.FULL_DAY);

        when(membershipTierService.create(any(MembershipTierRequest.class)))
            .thenReturn(tier);

        mockMvc.perform(post("/api/v1/membership-tiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Mensual completo",
                      "price": 45.00,
                      "durationDays": 30,
                      "entryQuota": null,
                      "allowedServices": ["WEIGHT_ROOM", "CLASSES"],
                      "timeBand": "FULL_DAY"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.name").value("Mensual completo"))
            .andExpect(jsonPath("$.price").value(45.00))
            .andExpect(jsonPath("$.durationDays").value(30))
            .andExpect(jsonPath("$.entryQuota").doesNotExist())
            .andExpect(jsonPath("$.allowedServices.length()").value(2))
            .andExpect(jsonPath("$.timeBand").value("FULL_DAY"));

        verify(membershipTierService).create(any(MembershipTierRequest.class));
    }

    @Test
    void rejectsBlankNameBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/membership-tiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": " ",
                      "price": 45.00,
                      "durationDays": 30,
                      "entryQuota": null,
                      "allowedServices": ["WEIGHT_ROOM"],
                      "timeBand": "FULL_DAY"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(membershipTierService);
    }

    @Test
    void rejectsNegativePriceBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/membership-tiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Plan inválido",
                      "price": -1.00,
                      "durationDays": 30,
                      "entryQuota": null,
                      "allowedServices": ["WEIGHT_ROOM"],
                      "timeBand": "FULL_DAY"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(membershipTierService);
    }
}