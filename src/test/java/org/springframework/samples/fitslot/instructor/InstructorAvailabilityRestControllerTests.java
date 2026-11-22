
package org.springframework.samples.fitslot.instructor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InstructorAvailabilityRestControllerTests {

    private static final String URL = "/api/v1/instructors/me/availability";

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

    @Mock
    private InstructorAvailabilityService service;

    private MockMvc mockMvc;

    private InstructorAvailability slot;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new InstructorAvailabilityRestController(service))
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        slot = new InstructorAvailability();
        slot.setId(101);
        slot.setDate(MONDAY);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(12, 0));
    }

    private String validJson() {
        return """
                {
                  "date": "2026-11-23",
                  "startTime": "09:00",
                  "endTime": "12:00"
                }
                """;
    }

    @Test
    void shouldReturnWeeklyAvailability() throws Exception {
        when(service.getMyAvailability(MONDAY))
                .thenReturn(List.of(slot));

        mockMvc.perform(get(URL)
                .param("weekStart", "2026-11-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].date")
                        .value("2026-11-23"))
                .andExpect(jsonPath("$[0].startTime")
                        .value("09:00:00"))
                .andExpect(jsonPath("$[0].endTime")
                        .value("12:00:00"));
    }

    @Test
    void shouldCreateAvailability() throws Exception {
        when(service.addAvailability(
                any(AvailabilitySlotRequest.class)))
                .thenReturn(slot);

        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.date")
                        .value("2026-11-23"));

        verify(service).addAvailability(
                any(AvailabilitySlotRequest.class));
    }

    @Test
    void shouldUpdateAvailability() throws Exception {
        when(service.updateAvailability(
                eq(101),
                any(AvailabilitySlotRequest.class)))
                .thenReturn(slot);

        mockMvc.perform(put(URL + "/101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101));

        verify(service).updateAvailability(
                eq(101),
                any(AvailabilitySlotRequest.class));
    }

    @Test
    void shouldDeleteAvailability() throws Exception {
        mockMvc.perform(delete(URL + "/101"))
                .andExpect(status().isNoContent());

        verify(service).deleteAvailability(101);
    }

    @Test
    void shouldReturn409WhenSlotsOverlap() throws Exception {
        when(service.addAvailability(
                any(AvailabilitySlotRequest.class)))
                .thenThrow(new IllegalStateException(
                        "Availability overlaps with an existing time slot"));

        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Availability overlaps with an existing time slot"));
    }

    @Test
    void shouldReturn400ForInvalidPeriod() throws Exception {
        when(service.addAvailability(
                any(AvailabilitySlotRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Start time must be before end time"));

        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "date": "2026-11-23",
                          "startTime": "15:00",
                          "endTime": "12:00"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Start time must be before end time"));
    }

    @Test
    void shouldReturn404WhenAvailabilityNotFound()
            throws Exception {

        when(service.updateAvailability(
                eq(999),
                any(AvailabilitySlotRequest.class)))
                .thenThrow(new NoSuchElementException(
                        "Availability not found"));

        mockMvc.perform(put(URL + "/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Availability not found"));
    }

    @Test
    void shouldRejectWeekStartThatIsNotMonday()
            throws Exception {

        when(service.getMyAvailability(
                LocalDate.of(2026, 11, 25)))
                .thenThrow(new IllegalArgumentException(
                        "Week start must be a Monday"));

        mockMvc.perform(get(URL)
                .param("weekStart", "2026-11-25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Week start must be a Monday"));
    }
}
