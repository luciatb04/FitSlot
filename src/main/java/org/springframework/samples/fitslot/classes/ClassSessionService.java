
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
    private final ClassReservationRepository classReservationRepository;

    public ClassSessionService(
            ClassSessionRepository classSessionRepository,
            ClassReservationRepository classReservationRepository) {

        this.classSessionRepository = classSessionRepository;
        this.classReservationRepository = classReservationRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassSession> getClassSessions() {
        return StreamSupport.stream(
                classSessionRepository.findAll().spliterator(),
                false)
                .toList();
    }

    // Retrieve all scheduled classes with availability
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getClassSessionResponses() {
        return getClassSessionResponses(null);
    }

    // Retrieve scheduled classes filtered by date
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getClassSessionResponses(
            LocalDate date) {

        List<ClassSession> sessions = date == null
                ? classSessionRepository.findAllByOrderByDateAscStartTimeAsc()
                : classSessionRepository.findByDateOrderByStartTimeAsc(date);

        return sessions.stream()
                .map(this::toResponse)
                .toList();
    }

    // Retrieve a specific class session with availability
    @Transactional(readOnly = true)
    public ClassSessionResponse getClassSessionResponse(Integer id) {
        return toResponse(getClassSession(id));
    }

    // Calculate available spots based on confirmed bookings
    private int calculateAvailableSpots(ClassSession session) {

        long confirmedBookings = classReservationRepository
                .countByClassSession_IdAndStatus(
                        session.getId(),
                        ReservationStatus.CONFIRMED);

        long availableSpots =
                (long) session.getMaxCapacity() - confirmedBookings;

        return (int) Math.max(0L, availableSpots);
    }

    // Convert entity to response including available spots
    private ClassSessionResponse toResponse(ClassSession session) {

        int availableSpots = calculateAvailableSpots(session);

        return ClassSessionResponse.fromEntity(
                session,
                availableSpots);
    }

    // Retrieve a class session by ID
    @Transactional(readOnly = true)
    public ClassSession getClassSession(Integer sessionId) {

        return classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class session not found: " + sessionId));
    }

    // Update the schedule of an existing class session
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

        if (newDate == null
                || newStartTime == null
                || newEndTime == null
                || newRoom == null) {

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
                        sessionId);

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
