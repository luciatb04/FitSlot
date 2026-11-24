package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ClassSessionRepository
        extends CrudRepository<ClassSession, Integer> {


    List<ClassSession> findByDateOrderByStartTimeAsc(LocalDate date);

    @Query("""
            SELECT COUNT(cs) FROM ClassSession cs
            WHERE cs.instructor.id = :instructorId
            AND cs.date = :date
            AND cs.id <> :sessionId
            AND cs.startTime < :endTime
            AND cs.endTime > :startTime
            """)
    long countInstructorConflicts(
            @Param("instructorId") Integer instructorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("sessionId") Integer sessionId);


    List<ClassSession> findAllByOrderByDateAscStartTimeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cs FROM ClassSession cs WHERE cs.id = :id")
    Optional<ClassSession> findByIdForUpdate(
            @Param("id") Integer id);

    @Query("""
            SELECT cs FROM ClassSession cs
            WHERE cs.room = :room
            AND cs.date = :date
            AND cs.gym.id = :gymId
            AND cs.id <> :sessionId
            AND cs.startTime < :endTime
            AND cs.endTime > :startTime
            """)
    List<ClassSession> findConflictingSessions(
            @Param("room") Room room,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("gymId") Integer gymId,
            @Param("sessionId") Integer sessionId);

    @Query("""
            SELECT cs FROM ClassSession cs
            WHERE cs.room = :room
            AND cs.date = :date
            AND cs.gym.id = :gymId
            AND cs.startTime < :endTime
            AND cs.endTime > :startTime
            """)
    List<ClassSession> findConflictingSessionsForNewSchedule(
            @Param("room") Room room,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("gymId") Integer gymId);
}
