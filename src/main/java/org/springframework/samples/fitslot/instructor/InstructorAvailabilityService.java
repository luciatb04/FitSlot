
package org.springframework.samples.fitslot.instructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.samples.fitslot.user.User;
import org.springframework.samples.fitslot.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class InstructorAvailabilityService {

    private final InstructorAvailabilityRepository availabilityRepository;
    private final InstructorRepository instructorRepository;
    private final UserService userService;
    private final EntityManager entityManager;

    public InstructorAvailabilityService(
            InstructorAvailabilityRepository availabilityRepository,
            InstructorRepository instructorRepository,
            UserService userService,
            EntityManager entityManager) {

        this.availabilityRepository = availabilityRepository;
        this.instructorRepository = instructorRepository;
        this.userService = userService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<InstructorAvailability> getMyAvailability(
            LocalDate weekStart) {

        if (weekStart == null ||
                weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException(
                    "Week start must be a Monday");
        }

        Instructor instructor = getCurrentInstructor();

        return availabilityRepository
                .findByInstructor_IdAndDateBetweenOrderByDateAscStartTimeAsc(
                        instructor.getId(),
                        weekStart,
                        weekStart.plusDays(6));
    }

    @Transactional
    public InstructorAvailability addAvailability(
            AvailabilitySlotRequest request) {

        validatePeriod(request);

        Instructor instructor = lockCurrentInstructor();

        checkOverlaps(instructor.getId(), request, null);

        InstructorAvailability availability = new InstructorAvailability();

        availability.setInstructor(instructor);
        availability.setDate(request.date());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());

        return availabilityRepository.save(availability);
    }

    @Transactional
    public InstructorAvailability updateAvailability(
            Integer availabilityId,
            AvailabilitySlotRequest request) {

        validatePeriod(request);

        Instructor instructor = lockCurrentInstructor();

        InstructorAvailability availability = findOwnedAvailability(instructor.getId(), availabilityId);

        checkOverlaps(instructor.getId(), request, availabilityId);

        availability.setDate(request.date());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());

        return availabilityRepository.save(availability);
    }

    @Transactional
    public void deleteAvailability(Integer availabilityId) {

        Instructor instructor = lockCurrentInstructor();

        InstructorAvailability availability = findOwnedAvailability(instructor.getId(), availabilityId);

        availabilityRepository.delete(availability);
    }

    private void validatePeriod(AvailabilitySlotRequest request) {

        if (request == null ||
                request.date() == null ||
                request.startTime() == null ||
                request.endTime() == null) {

            throw new IllegalArgumentException(
                    "Day, start time and end time are required");
        }

        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException(
                    "Start time must be before end time");
        }
    }

    private void checkOverlaps(
            Integer instructorId,
            AvailabilitySlotRequest request,
            Integer excludedId) {

        List<InstructorAvailability> existing = availabilityRepository
                .findByInstructor_IdAndDateOrderByStartTimeAsc(
                        instructorId,
                        request.date());

        for (InstructorAvailability slot : existing) {

            if (Objects.equals(slot.getId(), excludedId)) {
                continue;
            }

            boolean overlaps = request.startTime().isBefore(slot.getEndTime()) &&
                    request.endTime().isAfter(slot.getStartTime());

            if (overlaps) {
                throw new IllegalStateException(
                        "Availability overlaps with an existing time slot");
            }
        }
    }

    private InstructorAvailability findOwnedAvailability(
            Integer instructorId,
            Integer availabilityId) {

        InstructorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Availability not found"));

        if (!Objects.equals(
                availability.getInstructor().getId(),
                instructorId)) {

            throw new NoSuchElementException(
                    "Availability not found");
        }

        return availability;
    }

    private Instructor getCurrentInstructor() {

        User user = userService.findCurrentUser();

        return instructorRepository
                .findInstructorByUser(user.getId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Instructor profile not found"));
    }

    private Instructor lockCurrentInstructor() {

        Instructor instructor = getCurrentInstructor();

        entityManager.lock(
                instructor,
                LockModeType.PESSIMISTIC_WRITE);

        return instructor;
    }
}
