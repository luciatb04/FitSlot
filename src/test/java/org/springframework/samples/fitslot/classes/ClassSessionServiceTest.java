package org.springframework.samples.fitslot.classes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTests {

    @Mock
    private ClassSessionRepository classSessionRepository;

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

    @Test
    void shouldUpdateClassSessionSuccessfully() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classSessionRepository.findConflictingSessions(
                ROOM, DATE, START, END, SESSION_ID)).thenReturn(List.of());

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

        assertThrows(IllegalArgumentException.class, () -> classSessionService.updateClassSession(
                SESSION_ID, DATE, START, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectNullDate() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> classSessionService.updateClassSession(
                SESSION_ID, null, START, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectNullRoom() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> classSessionService.updateClassSession(
                SESSION_ID, DATE, START, END, null));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectStartTimeAfterEndTime() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        LocalTime invalidStart = LocalTime.of(12, 0);

        assertThrows(IllegalArgumentException.class, () -> classSessionService.updateClassSession(
                SESSION_ID, DATE, invalidStart, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldRejectEqualStartAndEndTime() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> classSessionService.updateClassSession(
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
                ROOM, DATE, START, END, SESSION_ID)).thenReturn(List.of(conflictingSession));

        assertThrows(IllegalStateException.class, () -> classSessionService.updateClassSession(
                SESSION_ID, DATE, START, END, ROOM));

        verify(classSessionRepository, never())
                .save(any(ClassSession.class));
    }

    @Test
    void shouldAllowChangingRoomWhenAvailable() {
        when(classSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session));

        when(classSessionRepository.findConflictingSessions(
                Room.CLASS_ROOM_2, DATE, START, END, SESSION_ID)).thenReturn(List.of());

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
                SESSION_ID)).thenReturn(List.of());

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
                ROOM, DATE, START, END, SESSION_ID)).thenReturn(List.of(new ClassSession()));

        assertThrows(IllegalStateException.class, () -> classSessionService.updateClassSession(
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
}
