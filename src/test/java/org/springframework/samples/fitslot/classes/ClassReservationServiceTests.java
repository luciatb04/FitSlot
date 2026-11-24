package org.springframework.samples.fitslot.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberRepository;

@ExtendWith(MockitoExtension.class)
class ClassReservationServiceTests {

    @Mock
    private ClassReservationRepository reservationRepository;

    @Mock
    private ClassSessionRepository sessionRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ClassReservationService service;

    private ClassSession session;
    private Member member;

    @BeforeEach
    void setUp() {
        session = new ClassSession();
        session.setId(101);
        session.setMaxCapacity(1);

        member = new Member();
        member.setId(12);
    }

    @Test
    void shouldPutNewReservationOnWaitingListWhenCapacityIsFull() {
        when(sessionRepository.findByIdForUpdate(101))
                .thenReturn(Optional.of(session));
        when(memberRepository.findById(12)).thenReturn(Optional.of(member));
        when(reservationRepository.findByMember_IdAndClassSession_Id(12, 101))
                .thenReturn(Optional.empty());
        when(reservationRepository.countByClassSession_IdAndStatus(
                101, ReservationStatus.CONFIRMED))
                .thenReturn(1L, 1L);
        when(reservationRepository
                .findByClassSession_IdAndStatusOrderByReservationTimeAsc(
                        101, ReservationStatus.WAITING_LIST))
                .thenReturn(List.of());
        when(reservationRepository.save(any(ClassReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClassReservation reservation = service.reserveClass(12, 101);

        assertEquals(ReservationStatus.WAITING_LIST, reservation.getStatus());
        verify(reservationRepository, never()).save(argThat(saved ->
                saved.getStatus() == ReservationStatus.CONFIRMED));
    }

    @Test
    void shouldConfirmReservationWhenCapacityIsAvailable() {
        when(sessionRepository.findByIdForUpdate(101))
                .thenReturn(Optional.of(session));
        when(memberRepository.findById(12)).thenReturn(Optional.of(member));
        when(reservationRepository.findByMember_IdAndClassSession_Id(12, 101))
                .thenReturn(Optional.empty());
        when(reservationRepository.countByClassSession_IdAndStatus(
                101, ReservationStatus.CONFIRMED))
                .thenReturn(0L, 0L);
        when(reservationRepository
                .findByClassSession_IdAndStatusOrderByReservationTimeAsc(
                        101, ReservationStatus.WAITING_LIST))
                .thenReturn(List.of());
        when(reservationRepository.save(any(ClassReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClassReservation reservation = service.reserveClass(12, 101);

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }
}
