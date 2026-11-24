package org.springframework.samples.fitslot.classes;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ClassSessionControllerTests {

    @Mock
    private ClassSessionService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ClassSessionController(service))
                .build();
    }

    @Test
    void shouldCreateClassSessionAndReturnCreatedResponse() throws Exception {
        when(service.createClassSession(
                "Yoga",
                LocalDate.of(2026, 12, 1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                Room.CLASS_ROOM_1,
                18,
                7))
                .thenReturn(new ClassSessionResponse(
                        110,
                        "Yoga",
                        Room.CLASS_ROOM_1,
                        LocalDate.of(2026, 12, 1),
                        DayOfWeek.TUESDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        18,
                        0,
                        null,
                        null,
                        18));

        mockMvc.perform(post("/api/v1/class-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Yoga",
                          "room": "CLASS_ROOM_1",
                          "date": "2026-12-01",
                          "startTime": "10:00",
                          "endTime": "11:00",
                          "maxCapacity": 18,
                          "gymId": 7
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(110))
                .andExpect(jsonPath("$.name").value("Yoga"))
                .andExpect(jsonPath("$.room").value("CLASS_ROOM_1"))
                .andExpect(jsonPath("$.maxCapacity").value(18))
                .andExpect(jsonPath("$.confirmedBookings").value(0));
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/class-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "",
                          "room": "CLASS_ROOM_1",
                          "date": "2026-12-01",
                          "startTime": "10:00",
                          "endTime": "11:00",
                          "maxCapacity": 0,
                          "gymId": 7
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnConflictWhenRoomScheduleOverlaps() throws Exception {
        when(service.createClassSession(
                anyString(),
                any(),
                any(),
                any(),
                any(),
                anyInt(),
                anyInt()))
                .thenThrow(new IllegalStateException(
                        "The room is already occupied at this time"));

        mockMvc.perform(post("/api/v1/class-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Yoga",
                          "room": "CLASS_ROOM_1",
                          "date": "2026-12-01",
                          "startTime": "10:00",
                          "endTime": "11:00",
                          "maxCapacity": 18,
                          "gymId": 7
                        }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldUpdateClassCapacity() throws Exception {
        ClassSessionResponse response = new ClassSessionResponse(
                110,
                "Yoga",
                Room.CLASS_ROOM_1,
                LocalDate.of(2026, 12, 1),
                DayOfWeek.TUESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                12,
                5,
                null,
                null,
                7);
        when(service.updateClassCapacity(110, 12)).thenReturn(response);

        mockMvc.perform(
                put("/api/v1/class-sessions/110/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxCapacity": 12}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCapacity").value(12))
                .andExpect(jsonPath("$.confirmedBookings").value(5))
                .andExpect(jsonPath("$.availableSpots").value(7));
    }

    @Test
    void shouldRejectInvalidCapacityAtHttpBoundary() throws Exception {
        mockMvc.perform(
                put("/api/v1/class-sessions/110/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxCapacity": 0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnConflictWhenNewCapacityIsBelowConfirmedBookings()
            throws Exception {
        when(service.updateClassCapacity(110, 3))
                .thenThrow(new IllegalStateException(
                        "Capacity cannot be lower than the number of confirmed bookings (5)"));

        mockMvc.perform(
                put("/api/v1/class-sessions/110/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxCapacity": 3}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteClassSession() throws Exception {
        mockMvc.perform(delete("/api/v1/class-sessions/110"))
                .andExpect(status().isNoContent());

        verify(service).deleteClassSession(110);
    }

    @Test
    void shouldReturnConflictWhenDeletingClassWithReservations()
            throws Exception {
        doThrow(new IllegalStateException(
                "Class sessions with reservations cannot be deleted"))
                .when(service).deleteClassSession(110);

        mockMvc.perform(delete("/api/v1/class-sessions/110"))
                .andExpect(status().isConflict());
    }
}
