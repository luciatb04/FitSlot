package org.springframework.samples.fitslot.classes;

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
import org.springframework.samples.fitslot.classes.InstructorSubstitution.AvailableInstructorResponse;
import org.springframework.samples.fitslot.classes.InstructorSubstitution.InstructorSubstitutionService;
import org.springframework.samples.fitslot.gym.Gym;
import org.springframework.samples.fitslot.gymowner.GymOwnerRepository;
import org.springframework.samples.fitslot.instructor.Instructor;
import org.springframework.samples.fitslot.instructor.InstructorAvailability;
import org.springframework.samples.fitslot.instructor.InstructorAvailabilityRepository;
import org.springframework.samples.fitslot.instructor.InstructorRepository;
import org.springframework.samples.fitslot.user.User;
import org.springframework.samples.fitslot.user.UserService;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class InstructorSubstitutionServiceTests {

    private static final Integer USER_ID = 2;
    private static final Integer GYM_ID = 1;
    private static final Integer SESSION_ID = 103;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 23);
    private static final LocalTime START = LocalTime.of(11, 0);
    private static final LocalTime END = LocalTime.of(12, 0);

    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private InstructorRepository instructorRepository;
    @Mock private InstructorAvailabilityRepository availabilityRepository;
    @Mock private GymOwnerRepository gymOwnerRepository;
    @Mock private UserService userService;

    @InjectMocks private InstructorSubstitutionService service;

    private Gym gym;
    private User manager;
    private Instructor currentInstructor;
    private ClassSession session;

    @BeforeEach
    void setUp() {
        gym = new Gym();
        gym.setId(GYM_ID);

        manager = new User();
        manager.setId(USER_ID);

        currentInstructor = instructor(1, "Linda", "Douglas", gym);

        session = new ClassSession();
        session.setId(SESSION_ID);
        session.setName("Spinning");
        session.setRoom(Room.CLASS_ROOM_1);
        session.setGym(gym);
        session.setInstructor(currentInstructor);
        session.setDate(DATE);
        session.setStartTime(START);
        session.setEndTime(END);
        session.setMaxCapacity(18);
    }

    private Instructor instructor(
            Integer id, String firstName, String lastName, Gym assignedGym) {
        Instructor instructor = new Instructor();
        instructor.setId(id);
        instructor.setFirstName(firstName);
        instructor.setLastName(lastName);
        instructor.setGym(assignedGym);
        instructor.setCity("Sevilla");
        return instructor;
    }

    private InstructorAvailability slot(Integer id, LocalTime start, LocalTime end) {
        InstructorAvailability availability = new InstructorAvailability();
        availability.setId(id);
        availability.setDate(DATE);
        availability.setStartTime(start);
        availability.setEndTime(end);
        return availability;
    }

    private void allowOwner() {
        when(userService.findCurrentUser()).thenReturn(manager);
        when(gymOwnerRepository.managesGym(USER_ID, GYM_ID)).thenReturn(true);
    }

    private void mockSessionForRead() {
        when(classSessionRepository.findById(SESSION_ID))
            .thenReturn(Optional.of(session));
    }

    private void mockSessionForWrite() {
        when(classSessionRepository.findByIdForUpdate(SESSION_ID))
            .thenReturn(Optional.of(session));
    }

    private void mockSubstitute(Instructor candidate) {
        when(instructorRepository.findByIdForUpdate(candidate.getId()))
            .thenReturn(Optional.of(candidate));
    }

    private void mockAvailability(Integer instructorId, InstructorAvailability... slots) {
        when(availabilityRepository.findByInstructor_IdAndDateOrderByStartTimeAsc(
                instructorId, DATE))
            .thenReturn(List.of(slots));
    }

@Test
void shouldListOnlyAvailableInstructorsWithoutConflicts() {
    mockSessionForRead();
    allowOwner();

    Instructor eligible = instructor(2, "Helen", "Leary", gym);
    Instructor incomplete = instructor(3, "James", "Carter", gym);
    Instructor occupied = instructor(4, "Rafael", "Ortega", gym);
    Instructor consecutiveSlots = instructor(5, "Henry", "Stevens", gym);

    when(instructorRepository.findByGym_Id(GYM_ID))
        .thenReturn(List.of(
            currentInstructor, eligible, incomplete, occupied, consecutiveSlots));

    mockAvailability(2, slot(1, LocalTime.of(10, 0), LocalTime.of(13, 0)));
    mockAvailability(3, slot(2, LocalTime.of(9, 0), LocalTime.of(11, 0)));
    mockAvailability(4, slot(3, LocalTime.of(10, 0), LocalTime.of(13, 0)));
    mockAvailability(5,
        slot(4, LocalTime.of(10, 0), LocalTime.of(11, 0)),
        slot(5, LocalTime.of(11, 0), LocalTime.of(12, 0)));

    // Simulate scheduling conflicts for all eligible candidates.
    // Only instructor 4 has a conflicting class.
    when(classSessionRepository.countInstructorConflicts(
            anyInt(),
            eq(DATE),
            eq(START),
            eq(END),
            eq(SESSION_ID)
    )).thenAnswer(invocation -> {

        Integer instructorId = invocation.getArgument(0);

        return instructorId.equals(4) ? 1L : 0L;
    });

    List<AvailableInstructorResponse> result =
        service.getAvailableInstructors(SESSION_ID);

    assertEquals(List.of(2, 5), result.stream()
        .map(AvailableInstructorResponse::id).toList());

    assertEquals("Helen", result.get(0).firstName());

    verify(availabilityRepository, never())
        .findByInstructor_IdAndDateOrderByStartTimeAsc(1, DATE);
}

    @Test
    void shouldListCandidatesWhenClassHasNoInstructor() {
        session.setInstructor(null);
        mockSessionForRead();
        allowOwner();

        Instructor eligible = instructor(2, "Helen", "Leary", gym);
        when(instructorRepository.findByGym_Id(GYM_ID))
            .thenReturn(List.of(eligible));
        mockAvailability(2, slot(1, START, END));

        List<AvailableInstructorResponse> result =
            service.getAvailableInstructors(SESSION_ID);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).id());
    }

    @Test
    void shouldRejectListingSubstitutesForAnotherManagersGym() {
        mockSessionForRead();
        when(userService.findCurrentUser()).thenReturn(manager);
        when(gymOwnerRepository.managesGym(USER_ID, GYM_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
            () -> service.getAvailableInstructors(SESSION_ID));

        verify(instructorRepository, never()).findByGym_Id(anyInt());
    }

    @Test
    void shouldSubstituteAnAssignedInstructor() {
        mockSessionForWrite();
        allowOwner();

        Instructor substitute = instructor(2, "Helen", "Leary", gym);
        mockSubstitute(substitute);
        mockAvailability(2, slot(1, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        service.substituteInstructor(SESSION_ID, 2);

        assertSame(substitute, session.getInstructor());
        verify(classSessionRepository).save(session);
        verify(classSessionRepository).countInstructorConflicts(
            2, DATE, START, END, SESSION_ID);
    }

    @Test
    void shouldAssignInstructorToInitiallyUnassignedClass() {
        session.setInstructor(null);
        mockSessionForWrite();
        allowOwner();

        Instructor substitute = instructor(2, "Helen", "Leary", gym);
        mockSubstitute(substitute);
        mockAvailability(2, slot(1, START, END));

        service.substituteInstructor(SESSION_ID, 2);

        assertEquals(2, session.getInstructor().getId());
        verify(classSessionRepository).save(session);
    }

    @Test
    void shouldAllowTwoAdjacentAvailabilitySlotsToCoverAClass() {
        mockSessionForWrite();
        allowOwner();

        Instructor substitute = instructor(2, "Helen", "Leary", gym);
        mockSubstitute(substitute);
        mockAvailability(2,
            slot(1, LocalTime.of(10, 0), LocalTime.of(11, 30)),
            slot(2, LocalTime.of(11, 30), LocalTime.of(12, 0)));

        service.substituteInstructor(SESSION_ID, 2);

        assertSame(substitute, session.getInstructor());
        verify(classSessionRepository).save(session);
    }

    @Test
    void shouldRejectSubstitutingWithCurrentInstructor() {
        mockSessionForWrite();
        allowOwner();

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.substituteInstructor(SESSION_ID, 1));

        assertTrue(ex.getMessage().contains("already assigned"));
        verify(instructorRepository, never()).findByIdForUpdate(anyInt());
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectSubstituteFromAnotherGym() {
        mockSessionForWrite();
        allowOwner();

        Gym anotherGym = new Gym();
        anotherGym.setId(2);
        Instructor substitute = instructor(2, "Helen", "Leary", anotherGym);
        mockSubstitute(substitute);

        assertThrows(IllegalArgumentException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verifyNoInteractions(availabilityRepository);
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectSubstituteWithoutRegisteredAvailability() {
        mockSessionForWrite();
        allowOwner();

        Instructor substitute = instructor(2, "Helen", "Leary", gym);
        mockSubstitute(substitute);
        mockAvailability(2);

        assertThrows(IllegalStateException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectPartiallyAvailableSubstitute() {
        mockSessionForWrite();
        allowOwner();

        Instructor substitute = instructor(2, "Helen", "Leary", gym);
        mockSubstitute(substitute);
        mockAvailability(2,
            slot(1, LocalTime.of(11, 30), LocalTime.of(13, 0)));

        assertThrows(IllegalStateException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectAvailabilityWithAGap() {
        mockSessionForWrite();
        allowOwner();

        Instructor substitute = instructor(2, "Helen", "Leary", gym);
        mockSubstitute(substitute);
        mockAvailability(2,
            slot(1, LocalTime.of(10, 0), LocalTime.of(11, 20)),
            slot(2, LocalTime.of(11, 30), LocalTime.of(13, 0)));

        assertThrows(IllegalStateException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectSubstituteAlreadyTeachingAnotherClass() {
        mockSessionForWrite();
        allowOwner();

        Instructor substitute = instructor(2, "Helen", "Leary", gym);
        mockSubstitute(substitute);
        mockAvailability(2, slot(1, LocalTime.of(10, 0), LocalTime.of(13, 0)));

        when(classSessionRepository.countInstructorConflicts(
            2, DATE, START, END, SESSION_ID)).thenReturn(1L);

        assertThrows(IllegalStateException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectAssignmentWhenManagerDoesNotOwnTheGym() {
        mockSessionForWrite();
        when(userService.findCurrentUser()).thenReturn(manager);
        when(gymOwnerRepository.managesGym(USER_ID, GYM_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verify(instructorRepository, never()).findByIdForUpdate(anyInt());
        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectNonexistentClass() {
        when(classSessionRepository.findByIdForUpdate(SESSION_ID))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verifyNoInteractions(instructorRepository, availabilityRepository);
    }

    @Test
    void shouldRejectNonexistentSubstitute() {
        mockSessionForWrite();
        allowOwner();
        when(instructorRepository.findByIdForUpdate(99))
            .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> service.substituteInstructor(SESSION_ID, 99));

        verify(classSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectNullSubstituteId() {
        assertThrows(IllegalArgumentException.class,
            () -> service.substituteInstructor(SESSION_ID, null));

        verifyNoInteractions(classSessionRepository, instructorRepository);
    }

    @Test
    void shouldRejectSessionWithoutGym() {
        session.setGym(null);
        mockSessionForWrite();

        assertThrows(IllegalStateException.class,
            () -> service.substituteInstructor(SESSION_ID, 2));

        verify(classSessionRepository, never()).save(any());
    }
}
