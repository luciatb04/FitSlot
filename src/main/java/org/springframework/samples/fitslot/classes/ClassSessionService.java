
package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.samples.fitslot.gym.Gym;
import org.springframework.samples.fitslot.gym.GymRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final ClassReservationRepository classReservationRepository;
    private final GymRepository gymRepository;

    public ClassSessionService(
            ClassSessionRepository classSessionRepository,
            ClassReservationRepository classReservationRepository,
            GymRepository gymRepository) {

        this.classSessionRepository = classSessionRepository;
        this.classReservationRepository = classReservationRepository;
        this.gymRepository = gymRepository;
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

    // Count confirmed bookings; waiting-list entries do not consume capacity.
    private long calculateConfirmedBookings(ClassSession session) {
        return classReservationRepository
                .countByClassSession_IdAndStatus(
                        session.getId(),
                        ReservationStatus.CONFIRMED);
    }

    // Convert entity to response including capacity and booking counts.
    private ClassSessionResponse toResponse(ClassSession session) {
        return ClassSessionResponse.fromEntity(
                session,
                calculateConfirmedBookings(session));
    }

    // Retrieve a class session by ID
    @Transactional(readOnly = true)
    public ClassSession getClassSession(Integer sessionId) {

        return classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class session not found: " + sessionId));
    }

    @Transactional
    public ClassSessionResponse createClassSession(
            String name,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Room room,
            Integer maxCapacity,
            Integer gymId) {

        if (name == null || name.isBlank()
                || date == null
                || startTime == null
                || endTime == null
                || room == null
                || maxCapacity == null
                || maxCapacity < 1
                || gymId == null) {
            throw new IllegalArgumentException(
                    "Class name, schedule, room, capacity and gym are required");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "Start time must be before end time");
        }

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gym not found: " + gymId));

        List<ClassSession> conflicts =
                classSessionRepository.findConflictingSessionsForNewSchedule(
                        room, date, startTime, endTime, gymId);

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "The room is already occupied at this time");
        }

        ClassSession session = new ClassSession();
        session.setName(name.trim());
        session.setDate(date);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setRoom(room);
        session.setMaxCapacity(maxCapacity);
        session.setGym(gym);

        return toResponse(classSessionRepository.save(session));
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
                        session.getGym().getId(),
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

    @Transactional
    public ClassSessionResponse updateClassCapacity(
            Integer sessionId,
            Integer newMaxCapacity) {

        if (newMaxCapacity == null || newMaxCapacity < 1) {
            throw new IllegalArgumentException(
                    "Maximum capacity must be a positive number");
        }

        ClassSession session = classSessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class session not found: " + sessionId));

        long confirmedBookings = calculateConfirmedBookings(session);
        if (newMaxCapacity < confirmedBookings) {
            throw new IllegalStateException(
                    "Capacity cannot be lower than the number of confirmed bookings ("
                            + confirmedBookings + ")");
        }

        session.setMaxCapacity(newMaxCapacity);
        return toResponse(classSessionRepository.save(session));
    }

    @Transactional
    public void deleteClassSession(Integer sessionId) {
        ClassSession session = classSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class session not found: " + sessionId));

        if (classReservationRepository.existsByClassSession_Id(sessionId)) {
            throw new IllegalStateException(
                    "Class sessions with reservations cannot be deleted");
        }

        classSessionRepository.delete(session);
    }
}
