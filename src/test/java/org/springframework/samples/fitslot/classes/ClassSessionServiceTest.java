
package org.springframework.samples.fitslot.classes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.samples.fitslot.instructor.Instructor;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTests {

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ClassReservationRepository classReservationRepository;

    @InjectMocks
    private ClassSessionService classSessionService;

    private ClassSession session;

    private final Integer SESSION_ID = 101;
    private final LocalDate DATE = LocalDate.of(2026, 11, 25);
    private final LocalTime START = LocalTime.of(10, 0);
    private final LocalTime END = LocalTime.of(11, 0);
    private final Room ROOM = Room.CLASS_ROOM_1;

    @BeforeEach
    void setUp() {
        session = new ClassSession();
        session.setId(SESSION_ID);
        session.setName("Yoga");
        session.setDate(LocalDate.of(2026, 11, 24));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(10, 0));
        session.setRoom(Room.CLASS_ROOM_2);
        session.setMaxCapacity(20);
    }

    // ==============================================
    // EXISTING TESTS: CLASS SCHEDULE MANAGEMENT
    // ==============================================

    @Test
    void shouldUpdateClassSessionSuccessfully() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classSessionRepository.findConflictingSessions(
                ROOM, DATE, START, END, SESSION_ID))
                .thenReturn(List.of());

        when(classSessionRepository.save(session))
                .thenReturn(session);

        ClassSession result = classSessionService.updateClassSession(
                SESSION_ID, DATE, START, END, ROOM);

        assertNotNull(result);
        assertEquals(DATE, result.getDate());
        assertEquals(START, result.getStartTime());
        assertEquals(END, result.getEndTime());
        assertEquals(ROOM, result.getRoom());

        verify(classSessionRepository).save(session);
    }

    @Test
    void shouldThrowExceptionWhenSessionDoesNotExist() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> classSessionService.updateClassSession(
                        SESSION_ID, DATE, START, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectNullDate() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        assertThrows(
                IllegalArgumentException.class,
                () -> classSessionService.updateClassSession(
                        SESSION_ID, null, START, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectNullRoom() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        assertThrows(
                IllegalArgumentException.class,
                () -> classSessionService.updateClassSession(
                        SESSION_ID, DATE, START, END, null));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectStartTimeAfterEndTime() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        LocalTime invalidStart = LocalTime.of(12, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> classSessionService.updateClassSession(
                        SESSION_ID, DATE, invalidStart, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectEqualStartAndEndTime() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        assertThrows(
                IllegalArgumentException.class,
                () -> classSessionService.updateClassSession(
                        SESSION_ID, DATE, START, START, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectRoomSchedulingConflict() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        ClassSession conflictingSession = new ClassSession();
        conflictingSession.setId(102);
        conflictingSession.setName("Pilates");
        conflictingSession.setRoom(ROOM);
        conflictingSession.setDate(DATE);
        conflictingSession.setStartTime(LocalTime.of(10, 30));
        conflictingSession.setEndTime(LocalTime.of(11, 30));

        when(classSessionRepository.findConflictingSessions(
                ROOM, DATE, START, END, SESSION_ID))
                .thenReturn(List.of(conflictingSession));

        assertThrows(
                IllegalStateException.class,
                () -> classSessionService.updateClassSession(
                        SESSION_ID, DATE, START, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldAllowChangingRoomWhenAvailable() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classSessionRepository.findConflictingSessions(
                Room.CLASS_ROOM_2, DATE, START, END, SESSION_ID))
                .thenReturn(List.of());

        when(classSessionRepository.save(session))
                .thenReturn(session);

        ClassSession result = classSessionService.updateClassSession(
                SESSION_ID, DATE, START, END, Room.CLASS_ROOM_2);

        assertEquals(Room.CLASS_ROOM_2, result.getRoom());

        verify(classSessionRepository).save(session);
    }

    @Test
    void shouldAllowUpdatingSessionWithoutSelfConflict() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        LocalDate originalDate = session.getDate();
        LocalTime originalStart = session.getStartTime();
        LocalTime originalEnd = session.getEndTime();
        Room originalRoom = session.getRoom();

        when(classSessionRepository.findConflictingSessions(
                originalRoom,
                originalDate,
                originalStart,
                originalEnd,
                SESSION_ID))
                .thenReturn(List.of());

        when(classSessionRepository.save(session))
                .thenReturn(session);

        ClassSession result = classSessionService.updateClassSession(
                SESSION_ID,
                originalDate,
                originalStart,
                originalEnd,
                originalRoom);

        assertNotNull(result);

        verify(classSessionRepository).save(session);
    }

    @Test
    void shouldNotModifyOriginalSessionWhenConflictOccurs() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classSessionRepository.findConflictingSessions(
                ROOM, DATE, START, END, SESSION_ID))
                .thenReturn(List.of(new ClassSession()));

        assertThrows(
                IllegalStateException.class,
                () -> classSessionService.updateClassSession(
                        SESSION_ID, DATE, START, END, ROOM));

        assertEquals(
                LocalDate.of(2026, 11, 24),
                session.getDate());

        assertEquals(
                Room.CLASS_ROOM_2,
                session.getRoom());

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    // ==============================================
    // UE-26: CHECK AVAILABLE CLASSES
    // ==============================================

    // TEST 11: Retrieve schedule and availability
    @Test
    void shouldReturnClassScheduleAndAvailability() {

        Instructor instructor = mock(Instructor.class);

        when(instructor.getId()).thenReturn(3);
        when(instructor.getFirstName()).thenReturn("Maria");
        when(instructor.getLastName()).thenReturn("Rossi");

        session.setInstructor(instructor);

        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classReservationRepository
                .countByClassSession_IdAndStatus(
                        SESSION_ID,
                        ReservationStatus.CONFIRMED))
                .thenReturn(15L);

        ClassSessionResponse response =
                classSessionService.getClassSessionResponse(SESSION_ID);

        assertAll(
                () -> assertEquals(SESSION_ID, response.id()),
                () -> assertEquals("Yoga", response.name()),
                () -> assertEquals(
                        LocalDate.of(2026, 11, 24),
                        response.date()),
                () -> assertEquals(
                        DayOfWeek.TUESDAY,
                        response.dayOfWeek()),
                () -> assertEquals(
                        LocalTime.of(9, 0),
                        response.startTime()),
                () -> assertEquals(
                        LocalTime.of(10, 0),
                        response.endTime()),
                () -> assertEquals(
                        Room.CLASS_ROOM_2,
                        response.room()),
                () -> assertEquals(3, response.instructorId()),
                () -> assertEquals(
                        "Maria Rossi",
                        response.instructorName()),
                () -> assertEquals(20, response.maxCapacity()),
                () -> assertEquals(5, response.availableSpots())
        );
    }

    // TEST 12: No bookings means all spots available
    @Test
    void shouldReturnFullCapacityWhenNoBookingsExist() {

        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        ClassSessionResponse response =
                classSessionService.getClassSessionResponse(SESSION_ID);

        assertEquals(20, response.availableSpots());

        verify(classReservationRepository)
                .countByClassSession_IdAndStatus(
                        SESSION_ID,
                        ReservationStatus.CONFIRMED);
    }

    // TEST 13: Full class means zero available spots
    @Test
    void shouldReturnZeroWhenClassIsFull() {

        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classReservationRepository
                .countByClassSession_IdAndStatus(
                        SESSION_ID,
                        ReservationStatus.CONFIRMED))
                .thenReturn(20L);

        ClassSessionResponse response =
                classSessionService.getClassSessionResponse(SESSION_ID);

        assertEquals(0, response.availableSpots());
    }

    // TEST 14: Available spots cannot be negative
    @Test
    void shouldNeverReturnNegativeAvailableSpots() {

        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classReservationRepository
                .countByClassSession_IdAndStatus(
                        SESSION_ID,
                        ReservationStatus.CONFIRMED))
                .thenReturn(25L);

        ClassSessionResponse response =
                classSessionService.getClassSessionResponse(SESSION_ID);

        assertEquals(0, response.availableSpots());
    }

    // TEST 15: Availability changes after booking/cancellation
    @Test
    void shouldUpdateAvailabilityAfterBookingAndCancellation() {

        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classReservationRepository
                .countByClassSession_IdAndStatus(
                        SESSION_ID,
                        ReservationStatus.CONFIRMED))
                .thenReturn(15L, 16L, 15L);

        ClassSessionResponse initial =
                classSessionService.getClassSessionResponse(SESSION_ID);

        ClassSessionResponse afterBooking =
                classSessionService.getClassSessionResponse(SESSION_ID);

        ClassSessionResponse afterCancellation =
                classSessionService.getClassSessionResponse(SESSION_ID);

        assertEquals(5, initial.availableSpots());
        assertEquals(4, afterBooking.availableSpots());
        assertEquals(5, afterCancellation.availableSpots());

        verify(classReservationRepository, times(3))
                .countByClassSession_IdAndStatus(
                        SESSION_ID,
                        ReservationStatus.CONFIRMED);
    }

    // TEST 16: Retrieve all scheduled classes
    @Test
    void shouldReturnAllScheduledClasses() {

        ClassSession pilates = new ClassSession();

        pilates.setId(102);
        pilates.setName("Pilates");
        pilates.setDate(DATE);
        pilates.setStartTime(LocalTime.of(11, 0));
        pilates.setEndTime(LocalTime.of(12, 0));
        pilates.setRoom(Room.CLASS_ROOM_1);
        pilates.setMaxCapacity(10);

        when(classSessionRepository
                .findAllByOrderByDateAscStartTimeAsc())
                .thenReturn(List.of(session, pilates));

        when(classReservationRepository
                .countByClassSession_IdAndStatus(
                        SESSION_ID,
                        ReservationStatus.CONFIRMED))
                .thenReturn(15L);

        when(classReservationRepository
                .countByClassSession_IdAndStatus(
                        102,
                        ReservationStatus.CONFIRMED))
                .thenReturn(8L);

        List<ClassSessionResponse> responses =
                classSessionService.getClassSessionResponses();

        assertEquals(2, responses.size());

        assertEquals("Yoga", responses.get(0).name());
        assertEquals(5, responses.get(0).availableSpots());

        assertEquals("Pilates", responses.get(1).name());
        assertEquals(2, responses.get(1).availableSpots());

        verify(classSessionRepository)
                .findAllByOrderByDateAscStartTimeAsc();
    }

    // TEST 17: Filter schedules by selected date
    @Test
    void shouldReturnClassesFilteredByDate() {

        LocalDate selectedDate = session.getDate();

        when(classSessionRepository
                .findByDateOrderByStartTimeAsc(selectedDate))
                .thenReturn(List.of(session));

        List<ClassSessionResponse> responses =
                classSessionService.getClassSessionResponses(selectedDate);

        assertEquals(1, responses.size());
        assertEquals(selectedDate, responses.get(0).date());

        verify(classSessionRepository)
                .findByDateOrderByStartTimeAsc(selectedDate);
    }

    // TEST 18: Handle nonexistent session
    @Test
    void shouldThrowExceptionWhenViewingNonexistentClass() {

        when(classSessionRepository.findById(999))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> classSessionService
                                .getClassSessionResponse(999)
                );

        assertEquals(
                "Class session not found: 999",
                exception.getMessage());

        verifyNoInteractions(classReservationRepository);
    }
}
