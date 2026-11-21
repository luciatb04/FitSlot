package org.springframework.samples.fitslot.classes;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ClassSessionRepositoryTests {

    @Autowired
    private ClassSessionRepository repository;

    private final LocalDate DATE = LocalDate.of(2026, 11, 25);

    private ClassSession createSession(
            Room room,
            LocalTime start,
            LocalTime end) {

        ClassSession session = new ClassSession();
        session.setName("Yoga");
        session.setRoom(room);
        session.setDate(DATE);
        session.setStartTime(start);
        session.setEndTime(end);
        session.setMaxCapacity(20);

        return repository.save(session);
    }

    @Test
    void shouldDetectOverlappingSessions() {
        ClassSession existing = createSession(
            Room.CLASS_ROOM_1,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0)
        );

        List<ClassSession> conflicts =
            repository.findConflictingSessions(
                Room.CLASS_ROOM_1,
                DATE,
                LocalTime.of(10, 30),
                LocalTime.of(11, 30),
                -1
            );

        assertEquals(1, conflicts.size());
        assertEquals(existing.getId(), conflicts.get(0).getId());
    }

    @Test
    void shouldAllowAdjacentSessions() {
        createSession(
            Room.CLASS_ROOM_1,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0)
        );

        List<ClassSession> conflicts =
            repository.findConflictingSessions(
                Room.CLASS_ROOM_1,
                DATE,
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                -1
            );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldAllowDifferentRoomsAtSameTime() {
        createSession(
            Room.CLASS_ROOM_1,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0)
        );

        List<ClassSession> conflicts =
            repository.findConflictingSessions(
                Room.CLASS_ROOM_2,
                DATE,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                -1
            );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldExcludeCurrentSession() {
        ClassSession existing = createSession(
            Room.CLASS_ROOM_1,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0)
        );

        List<ClassSession> conflicts =
            repository.findConflictingSessions(
                Room.CLASS_ROOM_1,
                DATE,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                existing.getId()
            );

        assertTrue(conflicts.isEmpty());
    }
}
