
package org.springframework.samples.fitslot.instructor;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class InstructorAvailabilityRepositoryTests {

    @Autowired
    private InstructorAvailabilityRepository availabilityRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    private Instructor instructor;

    private static final LocalDate MONDAY =
        LocalDate.of(2026, 11, 23);

    @BeforeEach
    void setUp() {
        instructor = new Instructor();
        instructor.setFirstName("James");
        instructor.setLastName("Carter");
        instructor.setCity("Sevilla");

        instructor = instructorRepository.save(instructor);
    }

    private InstructorAvailability saveSlot(
            Instructor owner,
            LocalDate date,
            int startHour,
            int endHour) {

        InstructorAvailability slot =
            new InstructorAvailability();

        slot.setInstructor(owner);
        slot.setDate(date);
        slot.setStartTime(LocalTime.of(startHour, 0));
        slot.setEndTime(LocalTime.of(endHour, 0));

        return availabilityRepository.save(slot);
    }

    @Test
    void shouldRetrieveAvailabilityForSpecificDate() {
        saveSlot(instructor, MONDAY, 9, 12);
        saveSlot(instructor, MONDAY.plusDays(1), 10, 13);

        List<InstructorAvailability> result =
            availabilityRepository
                .findByInstructor_IdAndDateOrderByStartTimeAsc(
                    instructor.getId(), MONDAY);

        assertEquals(1, result.size());
        assertEquals(MONDAY, result.get(0).getDate());
    }

    @Test
    void shouldRetrieveAvailabilityForSelectedWeek() {
        saveSlot(instructor, MONDAY, 9, 12);
        saveSlot(instructor, MONDAY.plusDays(2), 14, 17);
        saveSlot(instructor, MONDAY.plusWeeks(1), 9, 12);

        List<InstructorAvailability> result =
            availabilityRepository
                .findByInstructor_IdAndDateBetweenOrderByDateAscStartTimeAsc(
                    instructor.getId(),
                    MONDAY,
                    MONDAY.plusDays(6)
                );

        assertEquals(2, result.size());
        assertEquals(MONDAY, result.get(0).getDate());
        assertEquals(
            MONDAY.plusDays(2),
            result.get(1).getDate()
        );
    }

    @Test
    void shouldKeepDifferentWeeksIndependent() {
        saveSlot(instructor, MONDAY, 9, 12);
        saveSlot(instructor, MONDAY.plusWeeks(1), 9, 12);

        List<InstructorAvailability> firstWeek =
            availabilityRepository
                .findByInstructor_IdAndDateBetweenOrderByDateAscStartTimeAsc(
                    instructor.getId(),
                    MONDAY,
                    MONDAY.plusDays(6)
                );

        List<InstructorAvailability> secondWeek =
            availabilityRepository
                .findByInstructor_IdAndDateBetweenOrderByDateAscStartTimeAsc(
                    instructor.getId(),
                    MONDAY.plusWeeks(1),
                    MONDAY.plusWeeks(1).plusDays(6)
                );

        assertEquals(1, firstWeek.size());
        assertEquals(1, secondWeek.size());

        assertNotEquals(
            firstWeek.get(0).getDate(),
            secondWeek.get(0).getDate()
        );
    }

    @Test
    void shouldRetrieveOnlyCurrentInstructorsAvailability() {
        Instructor anotherInstructor = new Instructor();
        anotherInstructor.setFirstName("Helen");
        anotherInstructor.setLastName("Leary");
        anotherInstructor.setCity("Sevilla");

        anotherInstructor =
            instructorRepository.save(anotherInstructor);

        saveSlot(instructor, MONDAY, 9, 12);
        saveSlot(anotherInstructor, MONDAY, 10, 13);

        List<InstructorAvailability> result =
            availabilityRepository
                .findByInstructor_IdAndDateOrderByStartTimeAsc(
                    instructor.getId(), MONDAY);

        assertEquals(1, result.size());
        assertEquals(
            instructor.getId(),
            result.get(0).getInstructor().getId()
        );
    }

    @Test
    void shouldOrderSlotsByStartTime() {
        saveSlot(instructor, MONDAY, 16, 19);
        saveSlot(instructor, MONDAY, 9, 12);

        List<InstructorAvailability> result =
            availabilityRepository
                .findByInstructor_IdAndDateOrderByStartTimeAsc(
                    instructor.getId(), MONDAY);

        assertEquals(2, result.size());

        assertEquals(
            LocalTime.of(9, 0),
            result.get(0).getStartTime()
        );

        assertEquals(
            LocalTime.of(16, 0),
            result.get(1).getStartTime()
        );
    }
}
