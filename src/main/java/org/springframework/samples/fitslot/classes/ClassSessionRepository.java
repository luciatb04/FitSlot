package org.springframework.samples.fitslot.classes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository
        extends CrudRepository<ClassSession, Integer> {

    List<ClassSession> findByDateOrderByStartTimeAsc(LocalDate date);

    @Query("""
        SELECT cs FROM ClassSession cs
        WHERE cs.room = :room
        AND cs.date = :date
        AND cs.id <> :sessionId
        AND cs.startTime < :endTime
        AND cs.endTime > :startTime
        """)
    List<ClassSession> findConflictingSessions(
            @Param("room") Room room,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("sessionId") Integer sessionId
    );
}
