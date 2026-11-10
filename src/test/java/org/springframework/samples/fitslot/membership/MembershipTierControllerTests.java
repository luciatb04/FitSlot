package org.springframework.samples.fitslot.membership;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.samples.fitslot.configuration.SecurityConfiguration;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = MembershipTierController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebSecurityConfigurer.class),
        excludeAutoConfiguration = SecurityConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class MembershipTierControllerTests {

    private static final String BASE_URL = "/api/membership-tiers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MembershipTierService service;

    @Test
    void shouldGetAllTiers() throws Exception {
        MembershipTier tier = tier(1, "Premium");
        when(service.getAllTiers()).thenReturn(List.of(tier));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Premium"));

        verify(service).getAllTiers();
    }

    @Test
    void shouldGetServicesByTierId() throws Exception {
        when(service.getServices(1)).thenReturn(Set.of(ServiceType.CLASSES, ServiceType.POOL));

        mockMvc.perform(get(BASE_URL + "/1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder("CLASSES", "POOL")));

        verify(service).getServices(1);
    }

    @Test
    void shouldUpdateServicesByTierId() throws Exception {
        Set<ServiceType> services = Set.of(ServiceType.WEIGHT_ROOM, ServiceType.POOL);
        when(service.updateServices(1, services)).thenReturn(services);

        mockMvc.perform(put(BASE_URL + "/1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(services)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder(
                        "WEIGHT_ROOM", "POOL")));

        verify(service).updateServices(1, services);
    }

    @Test
    void shouldRejectNullServicesBody() throws Exception {
        assertThrows(ResponseStatusException.class,
                () -> new MembershipTierController(service).updateServices(1, null));

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundWhenServiceCannotFindTier() throws Exception {
        when(service.getServices(99)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Membership tier not found"));

        mockMvc.perform(get(BASE_URL + "/99/services"))
                .andExpect(status().isInternalServerError());

        verify(service).getServices(99);
    }

    @Test
    void shouldGetTimeBandByTierId() throws Exception {
        when(service.getTimeBand(1)).thenReturn(TimeBand.AFTERNOONS_ONLY);

        mockMvc.perform(get(BASE_URL + "/1/time-band"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AFTERNOONS_ONLY"));

        verify(service).getTimeBand(1);
    }

    @Test
    void shouldUpdateTimeBandByTierId() throws Exception {
        when(service.updateTimeBand(1, TimeBand.EVENINGS_ONLY))
                .thenReturn(TimeBand.EVENINGS_ONLY);

        mockMvc.perform(put(BASE_URL + "/1/time-band")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"EVENINGS_ONLY\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("EVENINGS_ONLY"));

        verify(service).updateTimeBand(1, TimeBand.EVENINGS_ONLY);
    }

    @Test
    void shouldRejectUnknownTimeBandConfiguration() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1/time-band")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"NOT_A_TIME_BAND\""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private MembershipTier tier(Integer id, String name) {
        MembershipTier tier = new MembershipTier();
        tier.setId(id);
        tier.setName(name);
        tier.setAllowedServices(Set.of(ServiceType.WEIGHT_ROOM));
        return tier;
    }
}
