package org.springframework.samples.fitslot.classes;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.samples.fitslot.classes.InstructorSubstitution.AvailableInstructorResponse;
import org.springframework.samples.fitslot.classes.InstructorSubstitution.InstructorSubstitutionController;
import org.springframework.samples.fitslot.classes.InstructorSubstitution.InstructorSubstitutionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InstructorSubstitutionControllerTests {

    private static final String URL = "/api/v1/class-sessions/103";

    @Mock private InstructorSubstitutionService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Test HTTP mapping and exception handlers, not Spring Security filters.
        mockMvc = MockMvcBuilders
            .standaloneSetup(new InstructorSubstitutionController(service))
            .build();
    }

    @Test
    void shouldReturnAvailableInstructors() throws Exception {
        when(service.getAvailableInstructors(103))
            .thenReturn(List.of(
                new AvailableInstructorResponse(2, "Helen", "Leary"),
                new AvailableInstructorResponse(4, "Rafael", "Ortega")
            ));

        mockMvc.perform(get(URL + "/available-instructors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].firstName").value("Helen"))
            .andExpect(jsonPath("$[0].lastName").value("Leary"))
            .andExpect(jsonPath("$[1].id").value(4));
    }

    @Test
    void shouldReturnEmptyListWhenNobodyIsAvailable() throws Exception {
        when(service.getAvailableInstructors(103))
            .thenReturn(List.of());

        mockMvc.perform(get(URL + "/available-instructors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldSaveSubstitution() throws Exception {
        mockMvc.perform(put(URL + "/instructor")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"instructorId": 2}
                    """))
            .andExpect(status().isNoContent());

        verify(service).substituteInstructor(103, 2);
    }

    @Test
    void shouldRejectMissingInstructorId() throws Exception {
        mockMvc.perform(put(URL + "/instructor")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"instructorId": null}
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturn400WhenSelectingCurrentInstructor() throws Exception {
        doThrow(new IllegalArgumentException(
            "The selected instructor is already assigned"))
            .when(service).substituteInstructor(103, 1);

        mockMvc.perform(put(URL + "/instructor")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"instructorId": 1}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("The selected instructor is already assigned"));
    }

    @Test
    void shouldReturn409WhenInstructorIsUnavailable() throws Exception {
        doThrow(new IllegalStateException(
            "The instructor is not available for this class"))
            .when(service).substituteInstructor(103, 2);

        mockMvc.perform(put(URL + "/instructor")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"instructorId": 2}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("The instructor is not available for this class"));
    }

    @Test
    void shouldReturn404WhenClassDoesNotExist() throws Exception {
        when(service.getAvailableInstructors(999))
            .thenThrow(new NoSuchElementException("Class session not found"));

        mockMvc.perform(get("/api/v1/class-sessions/999/available-instructors"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Class session not found"));
    }

    @Test
    void shouldReturn403WhenManagerDoesNotOwnGym() throws Exception {
        when(service.getAvailableInstructors(103))
            .thenThrow(new AccessDeniedException(
                "You are not authorized to manage this gym"));

        mockMvc.perform(get(URL + "/available-instructors"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                .value("You are not authorized to manage this gym"));
    }
}
