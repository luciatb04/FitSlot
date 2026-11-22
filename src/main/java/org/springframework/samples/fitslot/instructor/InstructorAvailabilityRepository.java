
package org.springframework.samples.fitslot.instructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface InstructorAvailabilityRepository
        extends CrudRepository<InstructorAvailability, Integer> {

    List<InstructorAvailability>
        findByInstructor_IdAndDateOrderByStartTimeAsc(
            Integer instructorId,
            LocalDate date
        );

    List<InstructorAvailability>
        findByInstructor_IdAndDateBetweenOrderByDateAscStartTimeAsc(
            Integer instructorId,
            LocalDate startDate,
            LocalDate endDate
        );
}
