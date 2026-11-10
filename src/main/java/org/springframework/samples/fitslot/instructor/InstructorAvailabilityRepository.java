// instructor/InstructorAvailabilityRepository.java
package org.springframework.samples.fitslot.instructor;

import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface InstructorAvailabilityRepository
        extends CrudRepository<InstructorAvailability, Integer> {
    List<InstructorAvailability>
        findByInstructor_IdAndDayOfWeekOrderByStartTimeAsc(
            Integer instructorId,
            DayOfWeek dayOfWeek
        );
}