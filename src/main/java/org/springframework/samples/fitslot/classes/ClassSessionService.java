package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;

    public ClassSessionService(
            ClassSessionRepository classSessionRepository) {
        this.classSessionRepository = classSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassSession> getClassSessions() {
        return StreamSupport.stream(
                classSessionRepository.findAll().spliterator(), false)
            .toList();
    }

    @Transactional(readOnly = true)
    public ClassSession getClassSession(Integer sessionId) {
        return classSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Class session not found: " + sessionId));
    }

    @Transactional
    public ClassSession updateClassSession(
            Integer sessionId,
            LocalDate newDate,
            LocalTime newStartTime,
            LocalTime newEndTime,
            Room newRoom) {

        ClassSession session = classSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Class session not found: " + sessionId));

        if (newDate == null || newStartTime == null ||
                newEndTime == null || newRoom == null) {
            throw new IllegalArgumentException(
                "Date, start time, end time and room are required");
        }

        if (!newStartTime.isBefore(newEndTime)) {
            throw new IllegalArgumentException(
                "Start time must be before end time");
        }

        List<ClassSession> conflicts =
            classSessionRepository.findConflictingSessions(
                newRoom,
                newDate,
                newStartTime,
                newEndTime,
                sessionId
            );

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                "The room is already occupied at this time");
        }

        session.setDate(newDate);
        session.setStartTime(newStartTime);
        session.setEndTime(newEndTime);
        session.setRoom(newRoom);

        return classSessionRepository.save(session);
    }
}
