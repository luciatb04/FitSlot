
package org.springframework.samples.fitslot.instructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.samples.fitslot.user.User;
import org.springframework.samples.fitslot.user.UserService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class InstructorAvailabilityServiceTests {

    private static final Integer USER_ID = 14;
    private static final Integer INSTRUCTOR_ID = 1;
    private static final Integer SLOT_ID = 101;

    private static final LocalDate MONDAY =
        LocalDate.of(2026, 11, 23);

    private static final LocalTime START =
        LocalTime.of(9, 0);

    private static final LocalTime END =
        LocalTime.of(12, 0);

    @Mock
    private InstructorAvailabilityRepository availabilityRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private UserService userService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InstructorAvailabilityService service;

    private Instructor instructor;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        instructor = new Instructor();
        instructor.setId(INSTRUCTOR_ID);
        instructor.setFirstName("James");
        instructor.setLastName("Carter");
        instructor.setCity("Sevilla");
        instructor.setUser(user);
    }

    private void mockCurrentInstructor() {
        when(userService.findCurrentUser())
            .thenReturn(user);

        when(instructorRepository.findInstructorByUser(USER_ID))
            .thenReturn(Optional.of(instructor));
    }

    private AvailabilitySlotRequest request(
            LocalDate date,
            LocalTime start,
            LocalTime end) {

        return new AvailabilitySlotRequest(date, start, end);
    }

    private InstructorAvailability createSlot(
            Integer id,
            Instructor owner,
            LocalDate date,
            LocalTime start,
            LocalTime end) {

        InstructorAvailability slot =
            new InstructorAvailability();

        slot.setId(id);
        slot.setInstructor(owner);
        slot.setDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);

        return slot;
    }

    @Test
    void shouldAddAvailabilitySuccessfully() {
        mockCurrentInstructor();

        AvailabilitySlotRequest input =
            request(MONDAY, START, END);

        when(availabilityRepository
            .findByInstructor_IdAndDateOrderByStartTimeAsc(
                INSTRUCTOR_ID, MONDAY))
            .thenReturn(List.of());

        when(availabilityRepository.save(
            any(InstructorAvailability.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        InstructorAvailability result =
            service.addAvailability(input);

        assertNotNull(result);
        assertEquals(instructor, result.getInstructor());
        assertEquals(MONDAY, result.getDate());
        assertEquals(START, result.getStartTime());
        assertEquals(END, result.getEndTime());

        verify(entityManager).lock(
            instructor, LockModeType.PESSIMISTIC_WRITE);

        verify(availabilityRepository).save(
            any(InstructorAvailability.class));
    }

    @Test
    void shouldRejectNullDate() {
        AvailabilitySlotRequest input =
            request(null, START, END);

        assertThrows(IllegalArgumentException.class,
            () -> service.addAvailability(input));

        verify(availabilityRepository, never())
            .save(any());
    }

    @Test
    void shouldRejectNullTimes() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addAvailability(
                request(MONDAY, null, END)));

        assertThrows(IllegalArgumentException.class,
            () -> service.addAvailability(
                request(MONDAY, START, null)));

        verify(availabilityRepository, never())
            .save(any());
    }

    @Test
    void shouldRejectEndTimeBeforeStartTime() {
        AvailabilitySlotRequest input = request(
            MONDAY,
            LocalTime.of(15, 0),
            LocalTime.of(12, 0)
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.addAvailability(input));

        verify(availabilityRepository, never())
            .save(any());
    }

    @Test
    void shouldRejectEqualStartAndEndTime() {
        AvailabilitySlotRequest input =
            request(MONDAY, START, START);

        assertThrows(IllegalArgumentException.class,
            () -> service.addAvailability(input));

        verify(availabilityRepository, never())
            .save(any());
    }

    @Test
    void shouldRejectOverlappingAvailability() {
        mockCurrentInstructor();

        InstructorAvailability existing = createSlot(
            SLOT_ID,
            instructor,
            MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(12, 0)
        );

        when(availabilityRepository
            .findByInstructor_IdAndDateOrderByStartTimeAsc(
                INSTRUCTOR_ID, MONDAY))
            .thenReturn(List.of(existing));

        AvailabilitySlotRequest input = request(
            MONDAY,
            LocalTime.of(10, 0),
            LocalTime.of(13, 0)
        );

        assertThrows(IllegalStateException.class,
            () -> service.addAvailability(input));

        verify(availabilityRepository, never())
            .save(any());
    }

    @Test
    void shouldAllowAdjacentTimeSlots() {
        mockCurrentInstructor();

        InstructorAvailability existing = createSlot(
            SLOT_ID,
            instructor,
            MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );

        when(availabilityRepository
            .findByInstructor_IdAndDateOrderByStartTimeAsc(
                INSTRUCTOR_ID, MONDAY))
            .thenReturn(List.of(existing));

        when(availabilityRepository.save(
            any(InstructorAvailability.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        InstructorAvailability result =
            service.addAvailability(request(
                MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
            ));

        assertNotNull(result);
        verify(availabilityRepository).save(any());
    }

    @Test
    void shouldAllowSameTimeOnDifferentWeeks() {
        mockCurrentInstructor();

        LocalDate nextMonday = MONDAY.plusWeeks(1);

        when(availabilityRepository
            .findByInstructor_IdAndDateOrderByStartTimeAsc(
                INSTRUCTOR_ID, nextMonday))
            .thenReturn(List.of());

        when(availabilityRepository.save(
            any(InstructorAvailability.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        InstructorAvailability result =
            service.addAvailability(request(
                nextMonday, START, END));

        assertEquals(nextMonday, result.getDate());

        verify(availabilityRepository)
            .findByInstructor_IdAndDateOrderByStartTimeAsc(
                INSTRUCTOR_ID, nextMonday);
    }

    @Test
    void shouldUpdateOwnAvailability() {
        mockCurrentInstructor();

        InstructorAvailability existing = createSlot(
            SLOT_ID, instructor, MONDAY, START, END);

        when(availabilityRepository.findById(SLOT_ID))
            .thenReturn(Optional.of(existing));

        LocalDate newDate = MONDAY.plusDays(2);
        LocalTime newStart = LocalTime.of(14, 0);
        LocalTime newEnd = LocalTime.of(17, 0);

        when(availabilityRepository
            .findByInstructor_IdAndDateOrderByStartTimeAsc(
                INSTRUCTOR_ID, newDate))
            .thenReturn(List.of());

        when(availabilityRepository.save(existing))
            .thenReturn(existing);

        InstructorAvailability result =
            service.updateAvailability(
                SLOT_ID,
                request(newDate, newStart, newEnd)
            );

        assertEquals(newDate, result.getDate());
        assertEquals(newStart, result.getStartTime());
        assertEquals(newEnd, result.getEndTime());

        verify(availabilityRepository).save(existing);
    }

    @Test
    void shouldRejectUpdatingAnotherInstructorsSlot() {
        mockCurrentInstructor();

        Instructor anotherInstructor = new Instructor();
        anotherInstructor.setId(2);

        InstructorAvailability otherSlot = createSlot(
            SLOT_ID, anotherInstructor, MONDAY, START, END);

        when(availabilityRepository.findById(SLOT_ID))
            .thenReturn(Optional.of(otherSlot));

        assertThrows(NoSuchElementException.class,
            () -> service.updateAvailability(
                SLOT_ID,
                request(MONDAY, START, END)
            ));

        verify(availabilityRepository, never())
            .save(any());
    }

    @Test
    void shouldDeleteOwnAvailability() {
        mockCurrentInstructor();

        InstructorAvailability existing = createSlot(
            SLOT_ID, instructor, MONDAY, START, END);

        when(availabilityRepository.findById(SLOT_ID))
            .thenReturn(Optional.of(existing));

        service.deleteAvailability(SLOT_ID);

        verify(availabilityRepository).delete(existing);
    }

    @Test
    void shouldRejectDeletingAnotherInstructorsSlot() {
        mockCurrentInstructor();

        Instructor anotherInstructor = new Instructor();
        anotherInstructor.setId(2);

        InstructorAvailability otherSlot = createSlot(
            SLOT_ID, anotherInstructor, MONDAY, START, END);

        when(availabilityRepository.findById(SLOT_ID))
            .thenReturn(Optional.of(otherSlot));

        assertThrows(NoSuchElementException.class,
            () -> service.deleteAvailability(SLOT_ID));

        verify(availabilityRepository, never())
            .delete(any());
    }

    @Test
    void shouldRetrieveAvailabilityForSelectedWeek() {
        mockCurrentInstructor();

        InstructorAvailability mondaySlot = createSlot(
            101, instructor, MONDAY, START, END);

        InstructorAvailability wednesdaySlot = createSlot(
            102, instructor, MONDAY.plusDays(2),
            LocalTime.of(14, 0), LocalTime.of(17, 0));

        when(availabilityRepository
            .findByInstructor_IdAndDateBetweenOrderByDateAscStartTimeAsc(
                INSTRUCTOR_ID,
                MONDAY,
                MONDAY.plusDays(6)
            ))
            .thenReturn(List.of(mondaySlot, wednesdaySlot));

        List<InstructorAvailability> result =
            service.getMyAvailability(MONDAY);

        assertEquals(2, result.size());
        assertEquals(MONDAY, result.get(0).getDate());
        assertEquals(
            MONDAY.plusDays(2), result.get(1).getDate());

        verify(availabilityRepository)
            .findByInstructor_IdAndDateBetweenOrderByDateAscStartTimeAsc(
                INSTRUCTOR_ID,
                MONDAY,
                MONDAY.plusDays(6)
            );
    }

    @Test
    void shouldRejectWeekStartThatIsNotMonday() {
        LocalDate wednesday = MONDAY.plusDays(2);

        assertThrows(IllegalArgumentException.class,
            () -> service.getMyAvailability(wednesday));

        verifyNoInteractions(availabilityRepository);
    }

    @Test
    void shouldRejectMissingInstructorProfile() {
        when(userService.findCurrentUser()).thenReturn(user);

        when(instructorRepository.findInstructorByUser(USER_ID))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> service.getMyAvailability(MONDAY));
    }
}
