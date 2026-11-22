
package org.springframework.samples.fitslot.classes;

import java.time.LocalDateTime;

import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberRepository;
import org.springframework.samples.fitslot.member.MemberStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassReservationService {

    private final ClassReservationRepository reservationRepository;
    private final ClassSessionRepository sessionRepository;
    private final MemberRepository memberRepository;

    public ClassReservationService(
            ClassReservationRepository reservationRepository,
            ClassSessionRepository sessionRepository,
            MemberRepository memberRepository) {

        this.reservationRepository = reservationRepository;
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public ClassReservation reserveClass(
            Integer memberId,
            Integer sessionId) {

        // Lock the session to prevent overbooking
        ClassSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class session not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Member is not active");
        }

        // Promote waiting members before accepting new bookings
        promoteWaitingMembers(session);

        ClassReservation reservation = reservationRepository
                .findByMember_IdAndClassSession_Id(
                        memberId, sessionId)
                .orElse(null);

        if (reservation != null &&
                reservation.getStatus() != ReservationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Member already has a reservation");
        }

        long confirmed = reservationRepository
                .countByClassSession_IdAndStatus(
                        sessionId,
                        ReservationStatus.CONFIRMED);

        if (reservation == null) {
            reservation = new ClassReservation();
            reservation.setMember(member);
            reservation.setClassSession(session);
        }

        reservation.setReservationTime(LocalDateTime.now());

        if (confirmed < session.getMaxCapacity()) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        } else {
            reservation.setStatus(ReservationStatus.WAITING_LIST);
        }

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(
            Integer memberId,
            Integer sessionId) {

        // Lock the session during cancellation
        ClassSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class session not found"));

        ClassReservation reservation = reservationRepository
                .findByMember_IdAndClassSession_Id(
                        memberId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Fill the free spot with the first waiting member
        promoteWaitingMembers(session);
    }

    private void promoteWaitingMembers(ClassSession session) {

        long confirmed = reservationRepository
                .countByClassSession_IdAndStatus(
                        session.getId(),
                        ReservationStatus.CONFIRMED);

        var waitingList = reservationRepository
                .findByClassSession_IdAndStatusOrderByReservationTimeAsc(
                        session.getId(),
                        ReservationStatus.WAITING_LIST);

        for (ClassReservation waiting : waitingList) {

            if (confirmed >= session.getMaxCapacity()) {
                break;
            }

            waiting.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(waiting);

            confirmed++;
        }
    }
}
