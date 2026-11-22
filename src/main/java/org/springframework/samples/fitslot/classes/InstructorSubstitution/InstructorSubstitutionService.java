package org.springframework.samples.fitslot.classes.InstructorSubstitution;

import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.samples.fitslot.classes.ClassSession;
import org.springframework.samples.fitslot.classes.ClassSessionRepository;
import org.springframework.samples.fitslot.gymowner.GymOwnerRepository;
import org.springframework.samples.fitslot.user.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.samples.fitslot.instructor.Instructor;
import org.springframework.samples.fitslot.instructor.InstructorRepository;
import org.springframework.samples.fitslot.instructor.InstructorAvailability;
import org.springframework.samples.fitslot.instructor.InstructorAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructorSubstitutionService {

    private final ClassSessionRepository classSessionRepository;
    private final GymOwnerRepository gymOwnerRepository;
    private final UserService userService;
    private final InstructorRepository instructorRepository;
    private final InstructorAvailabilityRepository availabilityRepository;

    public InstructorSubstitutionService(
            ClassSessionRepository classSessionRepository,
            InstructorRepository instructorRepository,
            InstructorAvailabilityRepository availabilityRepository,
            GymOwnerRepository gymOwnerRepository,
            UserService userService) {

        this.classSessionRepository = classSessionRepository;
        this.instructorRepository = instructorRepository;
        this.availabilityRepository = availabilityRepository;
        this.gymOwnerRepository = gymOwnerRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<AvailableInstructorResponse> getAvailableInstructors(
            Integer sessionId) {

        ClassSession session = findSession(sessionId);

        Integer gymId = getSessionGymId(session);

        validateGymOwnership(gymId);

        return instructorRepository.findByGym_Id(gymId)
                .stream()
                .filter(instructor -> !isCurrentInstructor(session, instructor))
                .filter(instructor -> coversClassSchedule(instructor, session))
                .filter(instructor -> !hasClassConflict(instructor.getId(), session))
                .map(AvailableInstructorResponse::fromEntity)
                .toList();
    }

    private void validateGymOwnership(Integer gymId) {

        Integer userId = userService.findCurrentUser().getId();

        boolean isOwner = gymOwnerRepository.managesGym(
                userId,
                gymId);

        if (!isOwner) {
            throw new AccessDeniedException(
                    "You are not authorized to manage this gym");
        }
    }

    @Transactional
    public void substituteInstructor(
            Integer sessionId,
            Integer newInstructorId) {

        if (newInstructorId == null) {
            throw new IllegalArgumentException(
                    "A substitute instructor is required");
        }

        ClassSession session = classSessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Class session not found"));

        Integer gymId = getSessionGymId(session);
        validateGymOwnership(gymId);

        if (session.getInstructor() != null &&
                Objects.equals(
                        session.getInstructor().getId(),
                        newInstructorId)) {

            throw new IllegalArgumentException(
                    "The selected instructor is already assigned");
        }

        Instructor substitute = instructorRepository
                .findByIdForUpdate(newInstructorId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Instructor not found"));

        if (substitute.getGym() == null ||
                !Objects.equals(
                        gymId,
                        substitute.getGym().getId())) {

            throw new IllegalArgumentException(
                    "The substitute must belong to the same gym");
        }

        if (!coversClassSchedule(substitute, session)) {
            throw new IllegalStateException(
                    "The instructor is not available for this class");
        }

        if (hasClassConflict(newInstructorId, session)) {
            throw new IllegalStateException(
                    "The instructor is already assigned to another class");
        }

        session.setInstructor(substitute);
        classSessionRepository.save(session);
    }

    private ClassSession findSession(Integer sessionId) {
        return classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Class session not found"));
    }

    private Integer getSessionGymId(ClassSession session) {
        if (session.getGym() == null) {
            throw new IllegalStateException(
                    "The class has no associated gym");
        }

        return session.getGym().getId();
    }

    private boolean isCurrentInstructor(
            ClassSession session,
            Instructor candidate) {

        return session.getInstructor() != null &&
                Objects.equals(
                        session.getInstructor().getId(),
                        candidate.getId());
    }

    private boolean coversClassSchedule(
            Instructor instructor,
            ClassSession session) {

        List<InstructorAvailability> slots = availabilityRepository
                .findByInstructor_IdAndDateOrderByStartTimeAsc(
                        instructor.getId(),
                        session.getDate());

        LocalTime coveredUntil = session.getStartTime();

        for (InstructorAvailability slot : slots) {

            // A gap in availability prevents full coverage
            if (slot.getStartTime().isAfter(coveredUntil)) {
                return false;
            }

            if (slot.getEndTime().isAfter(coveredUntil)) {
                coveredUntil = slot.getEndTime();
            }

            if (!coveredUntil.isBefore(session.getEndTime())) {
                return true;
            }
        }

        return false;
    }

    // Check for another class assigned at the same time
    private boolean hasClassConflict(
            Integer instructorId,
            ClassSession session) {

        return classSessionRepository.countInstructorConflicts(
                instructorId,
                session.getDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getId()) > 0;
    }
}
